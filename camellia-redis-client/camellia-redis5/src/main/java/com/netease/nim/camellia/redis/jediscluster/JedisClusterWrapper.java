package com.netease.nim.camellia.redis.jediscluster;

import com.netease.nim.camellia.redis.base.exception.CamelliaRedisException;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.*;
import redis.clients.jedis.providers.ClusterConnectionProvider;
import redis.clients.jedis.util.JedisClusterCRC16;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


/**
 *
 * Created by caojiajun on 2019/8/5.
 */
public class JedisClusterWrapper extends JedisCluster {

    private static final Logger logger = LoggerFactory.getLogger(JedisClusterWrapper.class);

    private static final Field providerField;
    private static final Field cacheField;
    private static final Field slotNodesField;
    private static final Field replicaSlotsField;

    private static final Method getHostAndPortMethod;
    static {
        try {
            providerField = JedisClusterWrapper.class.getDeclaredField("provider");
            providerField.setAccessible(true);

            cacheField = ClusterConnectionProvider.class.getDeclaredField("cache");
            cacheField.setAccessible(true);

            slotNodesField = JedisClusterInfoCache.class.getDeclaredField("slotNodes");
            slotNodesField.setAccessible(true);

            replicaSlotsField = JedisClusterInfoCache.class.getDeclaredField("replicaSlots");
            replicaSlotsField.setAccessible(true);

            getHostAndPortMethod = Connection.class.getDeclaredMethod("getHostAndPort");
            getHostAndPortMethod.setAccessible(true);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new CamelliaRedisException(e);
        }
    }

    private final ConcurrentHashMap<HostAndPort, JedisPool> jedisPoolMap = new ConcurrentHashMap<>();
    private final JedisClientConfig jedisClientConfig;

    private ClusterConnectionProvider provider;
    private JedisClusterInfoCache cache;

    public JedisClusterWrapper(Set<HostAndPort> clusterNodes, JedisClientConfig clientConfig, int maxAttempts,
                               GenericObjectPoolConfig<Connection> poolConfig) {
        super(clusterNodes, clientConfig, maxAttempts, poolConfig);
        this.jedisClientConfig = clientConfig;
        init();
    }

    private void init() {
        try {
            this.provider = (ClusterConnectionProvider) providerField.get(this);
            this.cache = (JedisClusterInfoCache) cacheField.get(provider);
        } catch (Exception e) {
            logger.error("init error", e);
            throw new CamelliaRedisException(e);
        }
    }

    public Map<Integer, List<HostAndPort>> clusterSlotsSlaveNodes() {
        Map<String, ConnectionPool> clusterNodes = getClusterNodes();
        Exception exception = null;
        for (Map.Entry<String, ConnectionPool> entry : clusterNodes.entrySet()) {
            ConnectionPool connectionPool = entry.getValue();
            try {
                try (Connection connection = connectionPool.getResource()){
                    return JedisClusterSlaveUtils.clusterSlotsSlaveNodes(connection);
                }
            } catch (Exception e) {
                exception = e;
            }
        }
        if (exception != null) {
            logger.error("clusterSlotsSlaveNodes error", exception);
            throw new CamelliaRedisException(exception);
        }
        throw new CamelliaRedisException("clusterSlotsSlaveNodes error");
    }

    public Jedis getJedis(byte[] key) {
        int slot = JedisClusterCRC16.getSlot(key);
        try (Connection connection = getConnectionFromSlot(slot)) {
            return getJedisFromConnection(connection);
        }
    }

    private Jedis getJedisFromConnection(Connection connection) {
        try {
            HostAndPort hostAndPort = (HostAndPort) getHostAndPortMethod.invoke(connection);
            JedisPool jedisPool = getJedisPool(hostAndPort);
            return jedisPool.getResource();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new CamelliaRedisException(e);
        }
    }

    public Jedis getSlaveJedis(byte[] key) {
        int slot = JedisClusterCRC16.getSlot(key);
        try (Connection connection = provider.getReplicaConnectionFromSlot(slot)) {
            try {
                HostAndPort hostAndPort = (HostAndPort) getHostAndPortMethod.invoke(connection);
                JedisPool jedisPool = getJedisPool(hostAndPort);
                return jedisPool.getResource();
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                throw new CamelliaRedisException(e);
            }
        }
    }

    public Jedis getJedis(HostAndPort hostAndPort) {
        JedisPool jedisPool = getJedisPool(hostAndPort);
        return jedisPool.getResource();
    }

    public HostAndPort getSlaveHostAndPort(int slot) {
        try (Connection connection = provider.getReplicaConnectionFromSlot(slot)) {
            try {
                return (HostAndPort) getHostAndPortMethod.invoke(connection);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                throw new CamelliaRedisException(e);
            }
        }
    }

    public List<Jedis> getJedisList() {
        try {
            HostAndPort[] slotNodes = (HostAndPort[]) slotNodesField.get(cache);
            List<Jedis> jedisList = new ArrayList<>();
            for (HostAndPort slotNode : slotNodes) {
                JedisPool jedisPool = getJedisPool(slotNode);
                jedisList.add(jedisPool.getResource());
            }
            return jedisList;
        } catch (Exception e) {
            throw new CamelliaRedisException(e);
        }
    }

    public List<Jedis> getSlaveJedisList() {
        try {
            List<ConnectionPool>[] replicaSlots = (List<ConnectionPool>[]) replicaSlotsField.get(cache);
            List<Jedis> jedisList = new ArrayList<>();
            for (List<ConnectionPool> replicaSlot : replicaSlots) {
                if (replicaSlot != null) {
                    for (ConnectionPool pool : replicaSlot) {
                        try (Connection connection = pool.getResource()){
                            Jedis jedis = getJedisFromConnection(connection);
                            jedisList.add(jedis);
                        }
                    }
                }
            }
            return jedisList;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new CamelliaRedisException(e);
        }
    }

    private JedisPool getJedisPool(HostAndPort hostAndPort) {
        JedisPool jedisPool = jedisPoolMap.get(hostAndPort);
        if (jedisPool == null) {
            synchronized (jedisPoolMap) {
                jedisPool = jedisPoolMap.get(hostAndPort);
                if (jedisPool == null) {
                    jedisPool = new JedisPool(hostAndPort, jedisClientConfig);
                    jedisPoolMap.put(hostAndPort, jedisPool);
                }
            }
        }
        return jedisPool;
    }

}
