package com.netease.nim.camellia.redis.proxy.netty;

import com.netease.nim.camellia.redis.proxy.command.async.RedisClient;
import com.netease.nim.camellia.redis.proxy.monitor.ChannelMonitor;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Queue;

/**
 *
 * Created by caojiajun on 2019/11/7.
 */
@ChannelHandler.Sharable
public class InitHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(InitHandler.class);

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        ChannelInfo channelInfo = ChannelInfo.init(ctx);
        ChannelMonitor.init(channelInfo);
        if (logger.isDebugEnabled()) {
            logger.debug("channel init, consid = {}", channelInfo.getConsid());
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        ChannelInfo channelInfo = ChannelInfo.get(ctx);
        if (channelInfo != null) {
            channelInfo.clear();
            ChannelMonitor.remove(channelInfo);
            Queue<RedisClient> queue = channelInfo.getRedisClientsForBlockingCommand();
            if (queue != null) {
                while (!queue.isEmpty()) {
                    RedisClient redisClient = queue.poll();
                    if (redisClient == null || !redisClient.isValid()) continue;
                    if (logger.isDebugEnabled()) {
                        logger.debug("redis client will close for blocking command interrupt by client, consid = {}, client.addr = {}, upstream.redis.client = {}",
                                channelInfo.getConsid(), ctx.channel().remoteAddress(), redisClient.getClientName());
                    }
                    redisClient.stop(true);
                }
            }
            RedisClient bindClient = channelInfo.getBindClient();
            if (bindClient != null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("bind redis client will close for disconnect, consid = {}, client.addr = {}, upstream.redis.client = {}",
                            channelInfo.getConsid(), ctx.channel().remoteAddress(), bindClient.getClientName());
                }
                bindClient.stop(true);
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
