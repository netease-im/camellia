package com.netease.nim.camellia.redis.jediscluster;

import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import redis.clients.jedis.BinaryJedis;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.InvalidURIException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.util.JedisURIHelper;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocketFactory;
import java.net.URI;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by caojiajun on 2024/11/11
 */
public class ReadOnlyJedisFactory implements PooledObjectFactory<Jedis> {

    private final AtomicReference<HostAndPort> hostAndPort = new AtomicReference<>();
    private final int connectionTimeout;
    private final int soTimeout;
    private final String password;
    private final int database;
    private final String clientName;
    private final boolean ssl;
    private final SSLSocketFactory sslSocketFactory;
    private final SSLParameters sslParameters;
    private final HostnameVerifier hostnameVerifier;

    public ReadOnlyJedisFactory(final String host, final int port, final int connectionTimeout,
                                final int soTimeout, final String password, final int database, final String clientName,
                                final boolean ssl, final SSLSocketFactory sslSocketFactory, final SSLParameters sslParameters,
                                final HostnameVerifier hostnameVerifier) {
        this.hostAndPort.set(new HostAndPort(host, port));
        this.connectionTimeout = connectionTimeout;
        this.soTimeout = soTimeout;
        this.password = password;
        this.database = database;
        this.clientName = clientName;
        this.ssl = ssl;
        this.sslSocketFactory = sslSocketFactory;
        this.sslParameters = sslParameters;
        this.hostnameVerifier = hostnameVerifier;
    }

    public ReadOnlyJedisFactory(final URI uri, final int connectionTimeout, final int soTimeout,
                                final String clientName, final boolean ssl, final SSLSocketFactory sslSocketFactory,
                                final SSLParameters sslParameters, final HostnameVerifier hostnameVerifier) {
        if (!JedisURIHelper.isValid(uri)) {
            throw new InvalidURIException(String.format("Cannot open Redis connection due invalid URI. %s", uri));
        }

        this.hostAndPort.set(new HostAndPort(uri.getHost(), uri.getPort()));
        this.connectionTimeout = connectionTimeout;
        this.soTimeout = soTimeout;
        this.password = JedisURIHelper.getPassword(uri);
        this.database = JedisURIHelper.getDBIndex(uri);
        this.clientName = clientName;
        this.ssl = ssl;
        this.sslSocketFactory = sslSocketFactory;
        this.sslParameters = sslParameters;
        this.hostnameVerifier = hostnameVerifier;
    }

    public void setHostAndPort(final HostAndPort hostAndPort) {
        this.hostAndPort.set(hostAndPort);
    }

    @Override
    public void activateObject(PooledObject<Jedis> pooledJedis) {
        final BinaryJedis jedis = pooledJedis.getObject();
        if (jedis.getDB() != database) {
            jedis.select(database);
            jedis.getClient().readonly();
            jedis.getClient().getStatusCodeReply();
        }
    }

    @Override
    public void destroyObject(PooledObject<Jedis> pooledJedis) {
        final BinaryJedis jedis = pooledJedis.getObject();
        if (jedis.isConnected()) {
            try {
                try {
                    jedis.quit();
                } catch (Exception ignore) {
                }
                jedis.disconnect();
            } catch (Exception ignore) {
            }
        }
    }

    @Override
    public PooledObject<Jedis> makeObject() {
        final HostAndPort hostAndPort = this.hostAndPort.get();
        final Jedis jedis = new Jedis(hostAndPort.getHost(), hostAndPort.getPort(), connectionTimeout,
                soTimeout, ssl, sslSocketFactory, sslParameters, hostnameVerifier);
        try {
            jedis.connect();
            if (password != null) {
                jedis.auth(password);
            }
            if (database != 0) {
                jedis.select(database);
            }
            if (clientName != null) {
                jedis.clientSetname(clientName);
            }
            jedis.readonly();
        } catch (JedisException je) {
            jedis.close();
            throw je;
        }

        return new DefaultPooledObject<>(jedis);
    }

    @Override
    public void passivateObject(PooledObject<Jedis> pooledJedis) {
    }

    @Override
    public boolean validateObject(PooledObject<Jedis> pooledJedis) {
        final BinaryJedis jedis = pooledJedis.getObject();
        try {
            HostAndPort hostAndPort = this.hostAndPort.get();

            String connectionHost = jedis.getClient().getHost();
            int connectionPort = jedis.getClient().getPort();

            return hostAndPort.getHost().equals(connectionHost)
                    && hostAndPort.getPort() == connectionPort && jedis.isConnected()
                    && jedis.ping().equals("PONG");
        } catch (Exception e) {
            return false;
        }
    }
}
