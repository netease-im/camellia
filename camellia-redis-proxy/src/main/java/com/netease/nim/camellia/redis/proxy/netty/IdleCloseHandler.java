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
        String consid = channelInfo == null ? null : channelInfo.getConsid();
        Long bid = channelInfo == null ? null : channelInfo.getBid();
        String bgroup = channelInfo == null ? null : channelInfo.getBgroup();
        boolean inSubscribe = channelInfo != null && channelInfo.isInSubscribe();
        if (evt.state() == IdleState.READER_IDLE) {
            logger.info("client connection reader idle, client.addr = {}, consid = {}", ctx.channel().remoteAddress(), consid);
            if (!inSubscribe && ProxyDynamicConf.getBoolean("reader.idle.connection.force.close.enable", bid, bgroup, false)) {
                logger.warn("client connection force close for reader idle, client.addr = {}, consid = {}, bid = {}, bgroup = {}",
                        ctx.channel().remoteAddress(), consid, bid, bgroup);
                ctx.close();
            }
        } else if (evt.state() == IdleState.WRITER_IDLE) {
            logger.info("client connection writer idle, client.addr = {}, consid = {}", ctx.channel().remoteAddress(), consid);
            if (!inSubscribe && ProxyDynamicConf.getBoolean("writer.idle.connection.force.close.enable", bid, bgroup, false)) {
                logger.warn("client connection force close for writer idle, client.addr = {}, consid = {}, bid = {}, bgroup = {}",
                        ctx.channel().remoteAddress(), consid, bid, bgroup);
                ctx.close();
            }
        } else if (evt.state() == IdleState.ALL_IDLE) {
            logger.info("client connection all idle, client.addr = {}, consid = {}", ctx.channel().remoteAddress(), consid);
            if (!inSubscribe && ProxyDynamicConf.getBoolean("all.idle.connection.force.close.enable", bid, bgroup, false)) {
                logger.warn("client connection force close for all idle, client.addr = {}, consid = {}, bid = {}, bgroup = {}",
                        ctx.channel().remoteAddress(), consid, bid, bgroup);
                ctx.close();
            }
        }
    }
}
