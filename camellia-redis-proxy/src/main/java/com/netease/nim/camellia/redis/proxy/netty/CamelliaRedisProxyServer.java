package com.netease.nim.camellia.redis.proxy.netty;

import com.netease.nim.camellia.redis.proxy.command.CommandInvoker;
import com.netease.nim.camellia.redis.proxy.conf.CamelliaServerProperties;
import com.netease.nim.camellia.redis.proxy.conf.Constants;
import com.netease.nim.camellia.redis.proxy.util.SocketUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * Created by caojiajun on 2019/11/5.
 */
public class CamelliaRedisProxyServer {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaRedisProxyServer.class);

    private final CamelliaServerProperties serverProperties;
    private final ServerHandler serverHandler;
    private final InitHandler initHandler = new InitHandler();
    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workGroup;
    private int port;

    public CamelliaRedisProxyServer(CamelliaServerProperties serverProperties, EventLoopGroup bossGroup, EventLoopGroup workGroup, CommandInvoker invoker) {
        this.serverProperties = serverProperties;
        this.serverHandler = new ServerHandler(invoker);
        this.bossGroup = bossGroup;
        this.workGroup = workGroup;
        if (bossGroup instanceof NioEventLoopGroup && workGroup instanceof NioEventLoopGroup) {
            GlobalRedisProxyEnv.bossThread = ((NioEventLoopGroup) bossGroup).executorCount();
            GlobalRedisProxyEnv.bossGroup = bossGroup;
            GlobalRedisProxyEnv.workThread = ((NioEventLoopGroup) workGroup).executorCount();
            GlobalRedisProxyEnv.workGroup = workGroup;
        }
    }

    public CamelliaRedisProxyServer(CamelliaServerProperties serverProperties, CommandInvoker invoker) {
        this.serverProperties = serverProperties;
        this.serverHandler = new ServerHandler(invoker);
        int bossThread = serverProperties.getBossThread();
        int workThread = serverProperties.getWorkThread();
        logger.info("CamelliaRedisProxyServer init, bossThread = {}, workThread = {}", bossThread, workThread);
        this.bossGroup = new NioEventLoopGroup(bossThread, new DefaultThreadFactory("boss-group"));
        this.workGroup = new NioEventLoopGroup(workThread, new DefaultThreadFactory("work-group"));
        GlobalRedisProxyEnv.workThread = workThread;
        GlobalRedisProxyEnv.bossThread = bossThread;
        GlobalRedisProxyEnv.workGroup = workGroup;
        GlobalRedisProxyEnv.bossGroup = bossGroup;
    }

    public void start() throws Exception {
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(bossGroup, workGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, serverProperties.getSoBacklog())
                .childOption(ChannelOption.SO_SNDBUF, serverProperties.getSoSndbuf())
                .childOption(ChannelOption.SO_RCVBUF, serverProperties.getSoRcvbuf())
                .childOption(ChannelOption.TCP_NODELAY, serverProperties.isTcpNoDelay())
                .childOption(ChannelOption.SO_KEEPALIVE, serverProperties.isSoKeepalive())
                .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK,
                        new WriteBufferWaterMark(serverProperties.getWriteBufferWaterMarkLow(), serverProperties.getWriteBufferWaterMarkHigh()))
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) {
                        ChannelPipeline p = ch.pipeline();
                        if (serverProperties.getReaderIdleTimeSeconds() >= 0 && serverProperties.getWriterIdleTimeSeconds() >= 0
                                && serverProperties.getAllIdleTimeSeconds() >= 0) {
                            p.addLast(new IdleCloseHandler(serverProperties.getReaderIdleTimeSeconds(),
                                    serverProperties.getWriterIdleTimeSeconds(), serverProperties.getAllIdleTimeSeconds()));
                        }
                        p.addLast(new CommandDecoder(serverProperties.getCommandDecodeMaxBatchSize(), serverProperties.getCommandDecodeBufferInitializerSize()));
                        p.addLast(new ReplyEncoder());
                        p.addLast(initHandler);
                        p.addLast(serverHandler);
                    }
                });
        int port = serverProperties.getPort();
        //如果设置为这个特殊的负数端口，则会随机选择一个可用的端口
        if (port == Constants.Server.serverPortRandSig) {
            port = SocketUtils.findRandomPort();
        }
        serverBootstrap.bind(port).sync();
        logger.info("CamelliaRedisProxyServer, so_backlog = {}, so_sendbuf = {}, so_rcvbuf = {}, so_keepalive = {}",
                serverProperties.getSoBacklog(), serverProperties.getSoSndbuf(), serverProperties.getSoRcvbuf(), serverProperties.isSoKeepalive());
        logger.info("CamelliaRedisProxyServer, tcp_no_delay = {}, write_buffer_water_mark_low = {}, write_buffer_water_mark_high = {}",
                serverProperties.isTcpNoDelay(), serverProperties.getWriteBufferWaterMarkLow(), serverProperties.getWriteBufferWaterMarkHigh());
        logger.info("CamelliaRedisProxyServer start at port: {}", port);
        GlobalRedisProxyEnv.port = port;
        this.port = port;
    }

    public int getPort() {
        return port;
    }
}
