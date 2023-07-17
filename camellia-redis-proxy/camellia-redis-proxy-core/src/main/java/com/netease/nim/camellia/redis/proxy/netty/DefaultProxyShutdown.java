package com.netease.nim.camellia.redis.proxy.netty;

import com.netease.nim.camellia.redis.proxy.upstream.*;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnection;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnectionHub;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


/**
 * Created by caojiajun on 2023/7/13
 */
public class DefaultProxyShutdown implements ProxyShutdown {

    private static final Logger logger = LoggerFactory.getLogger(DefaultProxyShutdown.class);

    private ChannelFuture serverChannelFuture;
    private ChannelFuture cportChannelFuture;
    private ChannelFuture consoleChannelFuture;
    private IUpstreamClientTemplateFactory factory;

    final void setServerChannelFuture(ChannelFuture serverChannelFuture) {
        this.serverChannelFuture = serverChannelFuture;
    }

    final void setCportChannelFuture(ChannelFuture cportChannelFuture) {
        this.cportChannelFuture = cportChannelFuture;
    }

    final void updateUpstreamClientTemplateFactory(IUpstreamClientTemplateFactory factory) {
        this.factory = factory;
    }

    public final void setConsoleChannelFuture(ChannelFuture consoleChannelFuture) {
        this.consoleChannelFuture = consoleChannelFuture;
    }

    @Override
    public synchronized boolean closeServer() {
        boolean success = true;
        if (serverChannelFuture != null) {
            try {
                serverChannelFuture.channel().close()
                        .addListener((ChannelFutureListener) channelFuture ->
                                logger.warn("camellia redis proxy server port shutdown for addr = {}", channelFuture.channel().localAddress()));
                serverChannelFuture = null;
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                success = false;
            }
        }
        if (cportChannelFuture != null) {
            try {
                cportChannelFuture.channel().close()
                        .addListener((ChannelFutureListener) channelFuture ->
                                logger.warn("camellia redis proxy server cport shutdown for addr = {}", channelFuture.channel().localAddress()));
                cportChannelFuture = null;
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                success = false;
            }
        }

        return success;
    }

    @Override
    public boolean closeConsoleServer() {
        boolean success = true;
        if (consoleChannelFuture != null) {
            try {
                consoleChannelFuture.channel().close()
                        .addListener((ChannelFutureListener) channelFuture ->
                                logger.warn("camellia redis proxy server console port shutdown for addr = {}", channelFuture.channel().localAddress()));
                consoleChannelFuture = null;
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                success = false;
            }
        }
        return success;
    }


    @Override
    public synchronized int closeRedisConnections() {
        Set<RedisConnection> allConnections = RedisConnectionHub.getInstance().getAllConnections();
        int size = 0;
        for (RedisConnection connection : allConnections) {
            if (connection.isValid()) {
                connection.stop(true);
                logger.warn("redis connection = {} closed", connection.getConnectionName());
                size++;
            }
        }
        return size;
    }

    @Override
    public int closeUpstreamClients() {
        if (factory == null) return 0;
        RedisProxyEnv redisProxyEnv = factory.getEnv();
        if (redisProxyEnv == null) return 0;
        UpstreamRedisClientFactory clientFactory = redisProxyEnv.getClientFactory();
        if (clientFactory == null) return 0;
        int size = 0;
        List<IUpstreamClient> upstreamClients = new ArrayList<>(clientFactory.getAll());
        for (IUpstreamClient client : upstreamClients) {
            IUpstreamClient upstreamClient = clientFactory.remove(client.getUrl());
            if (upstreamClient != null) {
                upstreamClient.shutdown();
                logger.warn("upstream client = {} shutdown", upstreamClient.getUrl());
                size ++;
            }
        }
        return size;
    }

    @Override
    public int closeUpstreamClientTemplates() {
        if (factory == null) return 0;
        return factory.shutdown();
    }
}
