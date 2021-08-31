package com.netease.nim.camellia.redis.proxy.netty;

import com.netease.nim.camellia.redis.proxy.command.CommandInvoker;
import com.netease.nim.camellia.redis.proxy.command.async.info.ProxyInfoUtils;
import com.netease.nim.camellia.redis.proxy.conf.CamelliaServerProperties;
import com.netease.nim.camellia.redis.proxy.conf.Constants;
import com.netease.nim.camellia.redis.proxy.util.ConfigInitUtil;
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
        ConfigInitUtil.initProxyDynamicConfHook(serverProperties);
        if (bossGroup instanceof NioEventLoopGroup && workGroup instanceof NioEventLoopGroup) {
            ProxyInfoUtils.updateThread(((NioEventLoopGroup) bossGroup).executorCount(), ((NioEventLoopGroup) workGroup).executorCount());
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
        ConfigInitUtil.initProxyDynamicConfHook(serverProperties);
        ProxyInfoUtils.updateThread(bossThread, workThread);
    }

    public void start() throws Exception {
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(bossGroup, workGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, serverProperties.getSoBacklog())
                .childOption(ChannelOption.SO_SNDBUF, serverProperties.getSoSndbuf())
                .childOption(ChannelOption.SO_RCVBUF, serverProperties.getSoRcvbuf())
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK,
                        new WriteBufferWaterMark(serverProperties.getWriteBufferWaterMarkLow(), serverProperties.getWriteBufferWaterMarkHigh()))
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline p = ch.pipeline();
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
        logger.info("CamelliaRedisProxyServer start at port: {}", port);
        ProxyInfoUtils.updatePort(port);
        this.port = port;
    }

    public int getPort() {
        return port;
    }
}
