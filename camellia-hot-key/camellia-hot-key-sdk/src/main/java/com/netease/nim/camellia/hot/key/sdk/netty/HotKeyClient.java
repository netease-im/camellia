package com.netease.nim.camellia.hot.key.sdk.netty;

import com.netease.nim.camellia.hot.key.common.netty.*;
import com.netease.nim.camellia.hot.key.common.netty.handler.HotKeyPackDecoder;
import com.netease.nim.camellia.hot.key.common.netty.handler.HotKeyPackEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by caojiajun on 2023/5/8
 */
public class HotKeyClient {

    private static final Logger logger = LoggerFactory.getLogger(HotKeyClient.class);

    private static final NioEventLoopGroup nioEventLoopGroup = new NioEventLoopGroup(HotKeyConstants.Client.workThread);

    private Channel channel = null;
    private RequestManager requestManager;
    private final AtomicLong requestIdGen = new AtomicLong(0);
    private volatile boolean valid;

    public HotKeyClient(HotKeyServerAddr addr, HotKeyPackConsumer consumer) {
        try {
            Bootstrap bootstrap = new Bootstrap()
                    .group(nioEventLoopGroup)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, HotKeyConstants.Client.TCP_NODELAY)
                    .option(ChannelOption.SO_KEEPALIVE, HotKeyConstants.Client.SO_KEEPALIVE)
                    .option(ChannelOption.SO_RCVBUF, HotKeyConstants.Client.SO_RCVBUF)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, HotKeyConstants.Client.CLIENT_CONNECT_TIMEOUT_MILLIS)
                    .handler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel channel) {
                            ChannelPipeline pipeLine = channel.pipeline();
                            pipeLine.addLast(HotKeyPackEncoder.getName(), new HotKeyPackEncoder()); // OUT
                            pipeLine.addLast(HotKeyPackDecoder.getName(), new HotKeyPackDecoder()); // IN
                            pipeLine.addLast(HotKeyPackHandler.getName(), new HotKeyPackHandler(requestManager, consumer)); // IN
                        }
                    });
            ChannelFuture f = bootstrap.connect(addr.getHost(), addr.getPort()).sync();
            channel = f.channel();
            channel.closeFuture().addListener((ChannelFutureListener) channelFuture -> {
                valid = false;
                logger.error("hot key client closed, addr = {}", addr, channelFuture.cause());
                stop();
            });
            requestManager = new RequestManager(channel.remoteAddress());
            valid = true;
        } catch (Exception e) {
            valid = false;
            stop();
            logger.error("hot key client start error, addr = {}", addr, e);
        }
    }

    private void stop() {
        if (requestManager != null) {
            requestManager.clear();
        }
    }

    public boolean isValid() {
        return valid;
    }

    public CompletableFuture<HotKeyPack> sendPack(HotKeyPack hotKeyPack) {
        hotKeyPack.getHeader().setRequestId(requestIdGen.incrementAndGet());
        CompletableFuture<HotKeyPack> future = requestManager.putSession(hotKeyPack);
        channel.writeAndFlush(hotKeyPack);
        return future;
    }
}
