package com.netease.nim.camellia.redis.proxy.netty;

import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnection;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnectionHub;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;


/**
 * Created by caojiajun on 2023/7/13
 */
public class ProxyShutdown {

    private static final Logger logger = LoggerFactory.getLogger(ProxyShutdown.class);

    private ChannelFuture serverChannelFuture;
    private ChannelFuture cportChannelFuture;
    private ChannelFuture consoleChannelFuture;
    final void setServerChannelFuture(ChannelFuture serverChannelFuture) {
        this.serverChannelFuture = serverChannelFuture;
    }

    final void setCportChannelFuture(ChannelFuture cportChannelFuture) {
        this.cportChannelFuture = cportChannelFuture;
    }

    public final void setConsoleChannelFuture(ChannelFuture consoleChannelFuture) {
        this.consoleChannelFuture = consoleChannelFuture;
    }

    /**
     * 关闭服务器的监听
     * @return 成功/失败
     */
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

    /**
     * 关闭服务器到redis的连接
     * @return 连接数
     */
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
}
