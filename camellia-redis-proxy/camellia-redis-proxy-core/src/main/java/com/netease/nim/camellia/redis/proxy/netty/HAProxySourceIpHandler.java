package com.netease.nim.camellia.redis.proxy.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.haproxy.HAProxyMessage;

/**
 * Created by caojiajun on 2023/8/23
 */
public class HAProxySourceIpHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof HAProxyMessage) {
            try {
                ChannelInfo channelInfo = ChannelInfo.get(ctx);
                HAProxyMessage haProxyMessage = (HAProxyMessage) msg;
                String sourceAddress = haProxyMessage.sourceAddress();
                int sourcePort = haProxyMessage.sourcePort();
                channelInfo.updateSourceAddr(sourceAddress, sourcePort);
            } finally {
                ((HAProxyMessage) msg).release();
            }
        } else {
            ctx.fireChannelRead(msg);
        }
    }
}
