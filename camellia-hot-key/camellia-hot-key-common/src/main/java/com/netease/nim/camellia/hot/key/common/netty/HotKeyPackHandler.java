package com.netease.nim.camellia.hot.key.common.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * Created by caojiajun on 2023/5/8
 */
public class HotKeyPackHandler extends ChannelInboundHandlerAdapter {

    public static String getName() {
        return "HotKeyPackHandler";
    }

    private final RequestManager manager;
    private final HotKeyPackConsumer handler;

    public HotKeyPackHandler(RequestManager manager, HotKeyPackConsumer handler) {
        this.manager = manager;
        this.handler = handler;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HotKeyPack) {
            HotKeyPack pack = (HotKeyPack) msg;
            if (pack.getHeader().isAck()) {
                manager.complete(pack);
            } else {
                handler.onPack(pack);
            }
        }
    }
}
