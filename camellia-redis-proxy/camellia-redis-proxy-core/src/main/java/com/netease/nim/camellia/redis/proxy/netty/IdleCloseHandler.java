package com.netease.nim.camellia.redis.proxy.netty;

import com.netease.nim.camellia.redis.proxy.command.CommandTaskQueue;
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
        boolean isCommandRunning = channelInfo != null && channelInfo.getCommandTaskQueue() != null && !channelInfo.getCommandTaskQueue().isEmpty();
        boolean checkInSubscribe = ProxyDynamicConf.getBoolean("idle.client.connection.force.close.check.in.subscribe", bid, bgroup, true);
        boolean checkCommandRunning = ProxyDynamicConf.getBoolean("idle.client.connection.force.close.check.command.running", bid, bgroup, false);
        logger.info("client connection {}, client.addr = {}, consid = {}, bid = {}, bgroup = {}",
                evt.state(), ctx.channel().remoteAddress(), consid, bid, bgroup);
        if (isCommandRunning && checkCommandRunning) {
            return;
        }
        if (inSubscribe && checkInSubscribe) {
            return;
        }
        if (evt.state() == IdleState.READER_IDLE) {
            if (ProxyDynamicConf.getBoolean("reader.idle.client.connection.force.close.enable", bid, bgroup, false)) {
                logger.warn("client connection force close for {}, client.addr = {}, consid = {}, bid = {}, bgroup = {}",
                        evt.state(), ctx.channel().remoteAddress(), consid, bid, bgroup);
                ctx.close();
            }
        } else if (evt.state() == IdleState.WRITER_IDLE) {
            if (ProxyDynamicConf.getBoolean("writer.idle.client.connection.force.close.enable", bid, bgroup, false)) {
                logger.warn("client connection force close for {}, client.addr = {}, consid = {}, bid = {}, bgroup = {}",
                        evt.state(), ctx.channel().remoteAddress(), consid, bid, bgroup);
                ctx.close();
            }
        } else if (evt.state() == IdleState.ALL_IDLE) {
            if (ProxyDynamicConf.getBoolean("all.idle.client.connection.force.close.enable", bid, bgroup, false)) {
                logger.warn("client connection force close for {}, client.addr = {}, consid = {}, bid = {}, bgroup = {}",
                        evt.state(), ctx.channel().remoteAddress(), consid, bid, bgroup);
                ctx.close();
            }
        }
    }
}
