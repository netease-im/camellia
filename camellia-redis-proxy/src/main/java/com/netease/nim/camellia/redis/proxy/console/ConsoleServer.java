package com.netease.nim.camellia.redis.proxy.console;

import com.netease.nim.camellia.core.util.SysUtils;
import com.netease.nim.camellia.redis.proxy.command.async.info.ProxyInfoUtils;
import com.netease.nim.camellia.redis.proxy.conf.Constants;
import com.netease.nim.camellia.redis.proxy.util.SocketUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.FutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsoleServer {

    private static final Logger logger = LoggerFactory.getLogger(ConsoleServer.class);
    private final int port;
    private final ConsoleService consoleService;

    public ConsoleServer(int port, ConsoleService consoleService) {
        if (port == Constants.Server.consolePortRandSig) {
            port = SocketUtils.findRandomPort();
        } else if (port < 0) {
            port = Constants.Server.consolePort;
        }
        this.port = port;
        this.consoleService = consoleService;
    }

    public void start() throws Exception {
        if (port == 0) return;
        new Thread(() -> {
            try {
                EventLoopGroup bossGroup = new NioEventLoopGroup(1, new DefaultThreadFactory("console-boss-group"));
                EventLoopGroup workerGroup = new NioEventLoopGroup(SysUtils.getCpuNum(), new DefaultThreadFactory("console-work-group"));
                try {
                    ServerBootstrap b = new ServerBootstrap();
                    b.option(ChannelOption.SO_BACKLOG, 1024);
                    b.group(bossGroup, workerGroup)
                            .channel(NioServerSocketChannel.class)
                            .childHandler(new ConsoleServerInitializer(new ConsoleServerHandler(consoleService)));
                    ChannelFuture channelFuture;
                    channelFuture = b.bind(port);
                    channelFuture.addListener((FutureListener<Void>) future -> {
                        if (future.isSuccess()) {
                            logger.info("Console Server start listen at port {}", port);
                            ProxyInfoUtils.updateConsolePort(port);
                        } else {
                            logger.error("Console Server start listen fail! at port {}, cause={}", port, future.cause());
                        }
                    });
                    Channel ch = channelFuture.sync().channel();
                    ch.closeFuture().sync();
                } finally {
                    bossGroup.shutdownGracefully();
                    workerGroup.shutdownGracefully();
                }
            } catch (Exception e) {
                logger.error("Console Server start error", e);
            }
        }).start();
    }

}
