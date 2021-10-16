package com.netease.nim.camellia.redis.proxy.netty;

import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by caojiajun on 2021/10/15
 */
public class IdleCloseHandler extends IdleStateHandler {

    private static final Logger logger = LoggerFactory.getLogger(IdleCloseHandler.class);

    public IdleCloseHandler(int readerIdleTimeSeconds, int writerIdleTimeSeconds, int allIdleTimeSeconds) {
        super(readerIdleTimeSeconds, writerIdleTimeSeconds, allIdleTimeSeconds);
    }

    @Override
    protected void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt) throws Exception {
        super.channelIdle(ctx, evt);
        ChannelInfo channelInfo = ChannelInfo.get(ctx);
        Long bid = channelInfo == null ? null : channelInfo.getBid();
        String bgroup = channelInfo == null ? null : channelInfo.getBgroup();
        if (evt.state() == IdleState.READER_IDLE) {
            logger.info("connection reader idle, client.addr = {}", ctx.channel().remoteAddress());
            if (ProxyDynamicConf.getBoolean("reader.idle.connection.force.close.enable", bid, bgroup, false)) {
                logger.warn("connection force close for reader idle, client.addr = {}, bid = {}, bgroup = {}",
                        ctx.channel().remoteAddress(), bid, bgroup);
                ctx.close();
            }
        } else if (evt.state() == IdleState.WRITER_IDLE) {
            logger.info("connection writer idle, client.addr = {}", ctx.channel().remoteAddress());
            if (ProxyDynamicConf.getBoolean("writer.idle.connection.force.close.enable", bid, bgroup, false)) {
                logger.warn("connection force close for reader idle, client.addr = {}, bid = {}, bgroup = {}",
                        ctx.channel().remoteAddress(), bid, bgroup);
            }
        } else if (evt.state() == IdleState.ALL_IDLE) {
            logger.info("connection all idle, client.addr = {}", ctx.channel().remoteAddress());
            if (ProxyDynamicConf.getBoolean("all.idle.connection.force.close.enable", bid, bgroup, false)) {
                logger.warn("connection force close for all idle, client.addr = {}, bid = {}, bgroup = {}",
                        ctx.channel().remoteAddress(), bid, bgroup);
                ctx.close();
            }
        }
    }
}
