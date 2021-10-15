package com.netease.nim.camellia.redis.proxy.netty;

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

    private final boolean readerIdleClose;
    private final boolean writerIdleClose;
    private final boolean allIdleClose;

    public IdleCloseHandler(int readerIdleTimeSeconds, int writerIdleTimeSeconds, int allIdleTimeSeconds,
                            boolean readerIdleClose, boolean writerIdleClose, boolean allIdleClose) {
        super(readerIdleTimeSeconds, writerIdleTimeSeconds, allIdleTimeSeconds);
        this.readerIdleClose = readerIdleClose;
        this.writerIdleClose = writerIdleClose;
        this.allIdleClose = allIdleClose;
    }

    @Override
    protected void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt) throws Exception {
        super.channelIdle(ctx, evt);
        ChannelInfo channelInfo = ChannelInfo.get(ctx);
        if (evt.state() == IdleState.READER_IDLE && readerIdleClose) {
            ctx.close();
            logger.warn("connect close for reader idle, consid = {}", channelInfo == null ? null : channelInfo.getConsid());
        } else if (evt.state() == IdleState.WRITER_IDLE && writerIdleClose) {
            ctx.close();
            logger.warn("connect close for writer idle, consid = {}", channelInfo == null ? null : channelInfo.getConsid());
        } else if (evt.state() == IdleState.ALL_IDLE && allIdleClose) {
            ctx.close();
            logger.warn("connect close for all idle, consid = {}", channelInfo == null ? null : channelInfo.getConsid());
        }
    }
}
