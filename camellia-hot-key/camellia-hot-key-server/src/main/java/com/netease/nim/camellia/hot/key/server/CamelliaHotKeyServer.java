package com.netease.nim.camellia.hot.key.server;

import com.netease.nim.camellia.hot.key.common.netty.HotKeyPackConsumer;
import com.netease.nim.camellia.hot.key.common.netty.HotKeyPackHandler;
import com.netease.nim.camellia.hot.key.common.netty.RequestManager;
import com.netease.nim.camellia.hot.key.common.netty.handler.HotKeyPackDecoder;
import com.netease.nim.camellia.hot.key.common.netty.handler.HotKeyPackEncoder;
import com.netease.nim.camellia.tools.utils.SysUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by caojiajun on 2023/5/6
 */
public class CamelliaHotKeyServer {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaHotKeyServer.class);

    private int port = 8080;

    private int boss = 1;
    private int work = SysUtils.getCpuHalfNum();

    private HotKeyPackConsumer consumer;

    public void start() {
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(new NioEventLoopGroup(boss, new DefaultThreadFactory("camellia-hot-key-server-boss")),
                            new NioEventLoopGroup(work, new DefaultThreadFactory("camellia-hot-key-server-work")))
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .childOption(ChannelOption.SO_SNDBUF, 10 * 1024 * 1024)
                    .childOption(ChannelOption.SO_RCVBUF, 10 * 1024 * 1024)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK,
                            new WriteBufferWaterMark(128 * 1024, 512 * 1024))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeLine = ch.pipeline();
                            pipeLine.addLast(HotKeyPackEncoder.getName(), new HotKeyPackEncoder()); // OUT
                            pipeLine.addLast(HotKeyPackDecoder.getName(), new HotKeyPackDecoder()); // IN
                            RequestManager requestManager = new RequestManager(ch.remoteAddress());
                            pipeLine.addLast(HotKeyPackHandler.getName(), new HotKeyPackHandler(requestManager, consumer)); // IN
                        }
                    });
            serverBootstrap.bind(port).sync();
            logger.info("CamelliaHotKeyServer start at port: {}", port);
        } catch (Exception e) {
            logger.error("CamelliaHotKeyServer start error", e);
            throw new IllegalStateException(e);
        }
    }
}
