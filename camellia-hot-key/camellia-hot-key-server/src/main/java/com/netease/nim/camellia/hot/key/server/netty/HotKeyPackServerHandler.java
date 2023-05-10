package com.netease.nim.camellia.hot.key.server.netty;

import com.netease.nim.camellia.hot.key.common.netty.HotKeyPack;
import com.netease.nim.camellia.hot.key.common.netty.HotKeyPackConsumer;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by caojiajun on 2023/5/8
 */
@ChannelHandler.Sharable
public class HotKeyPackServerHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(HotKeyPackServerHandler.class);

    public static String getName() {
        return "HotKeyPackServerHandler";
    }

    private final HotKeyPackConsumer handler;

    public HotKeyPackServerHandler(HotKeyPackConsumer handler) {
        this.handler = handler;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof HotKeyPack) {
            HotKeyPack pack = (HotKeyPack) msg;
            if (pack.getHeader().isAck()) {
                ChannelInfo channelInfo = ChannelInfo.get(ctx);
                if (channelInfo != null) {
                    channelInfo.getRequestManager().complete(pack);
                } else {
                    logger.warn("not found ChannelInfo in {}", ctx.channel().remoteAddress());
                }
            } else {
                handler.onPack(ctx.channel(), pack);
            }
        } else {
            logger.error("unknown pack, type = {}, value = {}", msg.getClass().getName(), msg);
        }
    }
}
