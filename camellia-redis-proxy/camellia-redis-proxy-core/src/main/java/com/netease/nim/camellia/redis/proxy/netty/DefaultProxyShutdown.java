package com.netease.nim.camellia.redis.proxy.netty;

import com.netease.nim.camellia.redis.proxy.monitor.PasswordMaskUtils;
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

    private final List<ChannelFuture> serverFutures = new ArrayList<>();
    private ChannelFuture cportFuture;
    private ChannelFuture consoleFuture;

    final synchronized void addServerFuture(ChannelFuture serverFuture) {
        serverFutures.add(serverFuture);
    }

    final synchronized void setCportFuture(ChannelFuture cportFuture) {
        this.cportFuture = cportFuture;
    }

    public synchronized final void setConsoleFuture(ChannelFuture consoleFuture) {
        this.consoleFuture = consoleFuture;
    }

    @Override
    public synchronized boolean closeServer() {
        boolean success = true;
        if (!serverFutures.isEmpty()) {
            try {
                for (ChannelFuture future : serverFutures) {
                    if (future == null) continue;
                    future.channel().close()
                            .addListener((ChannelFutureListener) channelFuture ->
                                    logger.warn("camellia redis proxy server port shutdown for addr = {}", channelFuture.channel().localAddress()));
                }
                serverFutures.clear();
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                success = false;
            }
        }
        if (cportFuture != null) {
            try {
                cportFuture.channel().close()
                        .addListener((ChannelFutureListener) channelFuture ->
                                logger.warn("camellia redis proxy server cport shutdown for addr = {}", channelFuture.channel().localAddress()));
                cportFuture = null;
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
        if (consoleFuture != null) {
            try {
                consoleFuture.channel().close()
                        .addListener((ChannelFutureListener) channelFuture ->
                                logger.warn("camellia redis proxy server console port shutdown for addr = {}", channelFuture.channel().localAddress()));
                consoleFuture = null;
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
        IUpstreamClientTemplateFactory factory = GlobalRedisProxyEnv.getClientTemplateFactory();
        if (factory == null) return 0;
        RedisProxyEnv redisProxyEnv = factory.getEnv();
        if (redisProxyEnv == null) return 0;
        UpstreamRedisClientFactory clientFactory = redisProxyEnv.getClientFactory();
        if (clientFactory == null) return 0;
        int size = 0;
        List<IUpstreamClient> upstreamClients = new ArrayList<>(clientFactory.getAll());
        for (IUpstreamClient client : upstreamClients) {
            IUpstreamClient upstreamClient = clientFactory.remove(client.getResource().getUrl());
            if (upstreamClient != null) {
                upstreamClient.shutdown();
                logger.warn("upstream client = {} shutdown", PasswordMaskUtils.maskResource(upstreamClient.getResource()));
                size ++;
            }
        }
        return size;
    }

    @Override
    public int closeUpstreamClientTemplates() {
        IUpstreamClientTemplateFactory factory = GlobalRedisProxyEnv.getClientTemplateFactory();
        if (factory == null) return 0;
        return factory.shutdown();
    }
}
