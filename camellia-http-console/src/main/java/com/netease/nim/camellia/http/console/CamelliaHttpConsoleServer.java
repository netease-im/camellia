package com.netease.nim.camellia.http.console;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Created by caojiajun on 2023/6/30
 */
public class CamelliaHttpConsoleServer {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaHttpConsoleServer.class);

    private final CamelliaHttpConsoleConfig config;
    private final Map<String, ConsoleApiInvoker> invokerMap;

    public CamelliaHttpConsoleServer(CamelliaHttpConsoleConfig config) {
        this.config = config;
        if (config.getPort() <= 0) {
            throw new IllegalArgumentException("console port <= 0");
        }
        if (config.getConsoleService() == null) {
            throw new IllegalArgumentException("console service is null");
        }
        this.invokerMap = ConsoleApiInvokersUtil.initApiInvokers(config.getConsoleService());
        logger.info("console service init, uri.set = {}", invokerMap.keySet());
    }

    public void start() {
        try {
            EventLoopGroup bossGroup = new NioEventLoopGroup(config.getBossThread(), new DefaultThreadFactory("console-boss-group"));
            EventLoopGroup workerGroup = new NioEventLoopGroup(config.getWorkThread(), new DefaultThreadFactory("console-work-group"));
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.option(ChannelOption.SO_BACKLOG, 1024);
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel channel) {
                            ChannelPipeline pipeline = channel.pipeline();
                            pipeline.addLast(new HttpServerCodec());
                            pipeline.addLast(new HttpObjectAggregator(61024));
                            pipeline.addLast(new CamelliaHttpConsoleServerHandler(invokerMap, config.getExecutor()));
                        }
                    });
            bootstrap.bind(config.getPort()).sync();
            logger.info("Console Server start listen at port {}", config.getPort());
        } catch (Exception e) {
            logger.error("camellia-http-console start error", e);
            throw new IllegalStateException("camellia-http-console start error", e);
        }
    }
}
