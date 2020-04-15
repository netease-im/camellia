package com.netease.nim.camellia.redis.proxy.console;

import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;

public class ConsoleServerInitializer extends ChannelInitializer<SocketChannel> {

    public ChannelInboundHandlerAdapter consoleHandler = null;

    public ConsoleServerInitializer(ChannelInboundHandlerAdapter handlerAdapter) {
        this.consoleHandler = handlerAdapter;
    }

    @Override
    public void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline p = ch.pipeline();
        p.addLast("codec", new HttpServerCodec());
        p.addLast("handler", consoleHandler);
    }
}
