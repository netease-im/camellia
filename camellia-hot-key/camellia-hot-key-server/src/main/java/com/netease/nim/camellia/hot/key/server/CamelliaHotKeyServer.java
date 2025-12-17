package com.netease.nim.camellia.hot.key.server;

import com.netease.nim.camellia.hot.key.common.exception.CamelliaHotKeyException;
import com.netease.nim.camellia.hot.key.common.netty.HotKeyPackConsumer;
import com.netease.nim.camellia.hot.key.common.netty.handler.HotKeyPackDecoder;
import com.netease.nim.camellia.hot.key.common.netty.handler.HotKeyPackEncoder;
import com.netease.nim.camellia.hot.key.server.conf.HotKeyServerProperties;
import com.netease.nim.camellia.hot.key.server.netty.HotKeyPackServerHandler;
import com.netease.nim.camellia.hot.key.server.netty.InitHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioIoHandler;
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

    private final InitHandler initHandler = new InitHandler();
    private final HotKeyPackServerHandler serverHandler;

    private final HotKeyServerProperties properties;

    public CamelliaHotKeyServer(HotKeyServerProperties properties) {
        this.properties = properties;
        HotKeyPackBizServerHandler bizHandler = new HotKeyPackBizServerHandler(properties);
        serverHandler = new HotKeyPackServerHandler(new HotKeyPackConsumer(bizHandler));
    }

    public void start() {
        try {
            EventLoopGroup boss = new MultiThreadIoEventLoopGroup(properties.getNettyBossThread(),
                    new DefaultThreadFactory("camellia-hot-key-server-boss"), NioIoHandler.newFactory());
            EventLoopGroup work = new MultiThreadIoEventLoopGroup(properties.getNettyWorkThread(),
                    new DefaultThreadFactory("camellia-hot-key-server-work"), NioIoHandler.newFactory());
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(boss, work)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, properties.getSoBacklog())
                    .childOption(ChannelOption.SO_SNDBUF, properties.getSoSndbuf())
                    .childOption(ChannelOption.SO_RCVBUF, properties.getSoRcvbuf())
                    .childOption(ChannelOption.TCP_NODELAY, properties.isTcpNoDelay())
                    .childOption(ChannelOption.SO_KEEPALIVE, properties.isSoKeepalive())
                    .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK,
                            new WriteBufferWaterMark(properties.getWriteBufferWaterMarkLow(), properties.getWriteBufferWaterMarkHigh()))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeLine = ch.pipeline();
                            pipeLine.addLast(HotKeyPackEncoder.getName(), new HotKeyPackEncoder()); // OUT
                            pipeLine.addLast(HotKeyPackDecoder.getName(), new HotKeyPackDecoder()); // IN
                            pipeLine.addLast(initHandler);
                            pipeLine.addLast(HotKeyPackServerHandler.getName(), serverHandler); // IN
                        }
                    });
            serverBootstrap.bind(properties.getPort()).sync();
            logger.info("CamelliaHotKeyServer start, boss-thread = {}, work-thread = {}", properties.getNettyBossThread(), properties.getNettyWorkThread());
            logger.info("CamelliaHotKeyServer start at port: {}", properties.getPort());
        } catch (Exception e) {
            logger.error("CamelliaHotKeyServer start error", e);
            throw new CamelliaHotKeyException(e);
        }
    }

    public int getPort() {
        return properties.getPort();
    }
}
