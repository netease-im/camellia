package com.netease.nim.camellia.redis.proxy.netty;

import com.netease.nim.camellia.redis.proxy.auth.ConnectLimiter;
import com.netease.nim.camellia.redis.proxy.monitor.ChannelMonitor;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnection;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import com.netease.nim.camellia.redis.proxy.util.ExecutorUtils;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollChannelOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 *
 * Created by caojiajun on 2019/11/7.
 */
@ChannelHandler.Sharable
public class InitHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(InitHandler.class);

    private final ChannelInfo.ChannelType channelType;

    public InitHandler(ChannelInfo.ChannelType channelType) {
        this.channelType = channelType;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        ChannelInfo channelInfo = ChannelInfo.init(ctx, channelType);
        int threshold = ConnectLimiter.connectThreshold();
        int currentConnect = ChannelMonitor.connect();
        if (currentConnect >= threshold) {
            channelInfo.setChannelStats(ChannelInfo.ChannelStats.INVALID);
            ChannelFuture channelFuture = ctx.channel().writeAndFlush(ErrorReply.TOO_MANY_CLIENTS);
            channelFuture.addListener((ChannelFutureListener) channelFuture1 -> {
                long delayMillis = ConnectLimiter.delayCloseMillis();
                if (delayMillis > 0) {
                    ExecutorUtils.submitDelayTask(() -> ctx.channel().close(), delayMillis, TimeUnit.MILLISECONDS);
                } else {
                    ctx.channel().close();
                }
            });
            logger.warn("too many connects, connect will be force closed, current = {}, max = {}, consid = {}, client.addr = {}",
                    currentConnect, threshold, channelInfo.getConsid(), ctx.channel().remoteAddress());
            return;
        }
        ChannelMonitor.init(channelInfo);
        if (logger.isDebugEnabled()) {
            logger.debug("channel init, consid = {}", channelInfo.getConsid());
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            if (GlobalRedisProxyEnv.isServerTcpQuickAckEnable() && channelType == ChannelInfo.ChannelType.tcp) {
                ctx.channel().config().setOption(EpollChannelOption.TCP_QUICKACK, Boolean.TRUE);
            }
        } catch (Exception e) {
            ErrorLogCollector.collect(InitHandler.class, "set TCP_QUICKACK error", e);
        }
        super.channelRead(ctx, msg);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        ChannelInfo channelInfo = ChannelInfo.get(ctx);
        if (channelInfo != null) {
            channelInfo.clear();
            ChannelMonitor.remove(channelInfo);
            ConcurrentHashMap<String, RedisConnection> map1 = channelInfo.getBindRedisConnectionCache();
            if (map1 != null) {
                for (Map.Entry<String, RedisConnection> entry : map1.entrySet()) {
                    RedisConnection redisConnection = entry.getValue();
                    if (redisConnection == null || !redisConnection.isValid()) continue;
                    if (logger.isInfoEnabled()) {
                        logger.info("bind redis connection cache will close interrupt by proxy, consid = {}, client.addr = {}, upstream.redis.connection = {}",
                                channelInfo.getConsid(), ctx.channel().remoteAddress(), redisConnection.getConnectionName());
                    }
                    redisConnection.stop(true);
                }
                map1.clear();
            }
            ConcurrentHashMap<String, RedisConnection> map2 = channelInfo.getBindSubscribeRedisConnectionCache();
            if (map2 != null) {
                for (Map.Entry<String, RedisConnection> entry : map2.entrySet()) {
                    RedisConnection redisConnection = entry.getValue();
                    if (redisConnection == null || !redisConnection.isValid()) continue;
                    if (logger.isInfoEnabled()) {
                        logger.info("bind subscribe redis connection cache will close interrupt by proxy, consid = {}, client.addr = {}, upstream.redis.connection = {}",
                                channelInfo.getConsid(), ctx.channel().remoteAddress(), redisConnection.getConnectionName());
                    }
                    redisConnection.stop(true);
                }
                map2.clear();
            }
            RedisConnection bindConnection = channelInfo.getBindConnection();
            if (bindConnection != null) {
                if (logger.isInfoEnabled()) {
                    logger.info("bind redis connection will close for disconnect, consid = {}, client.addr = {}, upstream.redis.connection = {}",
                            channelInfo.getConsid(), ctx.channel().remoteAddress(), bindConnection.getConnectionName());
                }
                bindConnection.stop(true);
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("channel close, consid = {}", channelInfo == null ? "null" : channelInfo.getConsid());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ChannelInfo channelInfo = ChannelInfo.get(ctx);
        if (logger.isDebugEnabled()) {
            logger.debug("channel error, consid = {}", channelInfo == null ? "null" : channelInfo.getConsid(), cause);
        }
    }
}
