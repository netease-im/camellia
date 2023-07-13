package com.netease.nim.camellia.redis.proxy.netty;

import com.netease.nim.camellia.redis.proxy.command.ICommandInvoker;
import com.netease.nim.camellia.redis.proxy.conf.CamelliaServerProperties;
import com.netease.nim.camellia.redis.proxy.conf.Constants;
import com.netease.nim.camellia.redis.proxy.info.ProxyInfoUtils;
import com.netease.nim.camellia.redis.proxy.util.SocketUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.socket.SocketChannel;
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
    private int port;

    public CamelliaRedisProxyServer(CamelliaServerProperties serverProperties, ICommandInvoker invoker) {
        GlobalRedisProxyEnv.init(serverProperties);
        this.serverProperties = serverProperties;
        this.serverHandler = new ServerHandler(invoker);
        this.serverHandler.setServerProperties(serverProperties);
        if (logger.isInfoEnabled()) {
            logger.info("CamelliaRedisProxyServer init, netty-transport-mode = {}, bossThread = {}, workThread = {}",
                    GlobalRedisProxyEnv.getNettyTransportMode(), GlobalRedisProxyEnv.getBossThread(), GlobalRedisProxyEnv.getWorkThread());
        }
    }

    public void start() throws Exception {
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(GlobalRedisProxyEnv.getBossGroup(), GlobalRedisProxyEnv.getWorkGroup())
                .channel(GlobalRedisProxyEnv.getServerChannelClass())
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
        if (GlobalRedisProxyEnv.isServerTcpQuickAckEnable()) {
            serverBootstrap.childOption(EpollChannelOption.TCP_QUICKACK, Boolean.TRUE);
        }
        int port = serverProperties.getPort();
        //如果设置为这个特殊的负数端口，则会随机选择一个可用的端口
        if (port == Constants.Server.serverPortRandSig) {
            port = SocketUtils.findRandomAvailablePort();
        }
        ChannelFuture future = serverBootstrap.bind(port).sync();
        logger.info("CamelliaRedisProxyServer, so_backlog = {}, so_sendbuf = {}, so_rcvbuf = {}, so_keepalive = {}",
                serverProperties.getSoBacklog(), serverProperties.getSoSndbuf(), serverProperties.getSoRcvbuf(), serverProperties.isSoKeepalive());
        logger.info("CamelliaRedisProxyServer, tcp_no_delay = {}, tcp_quick_ack = {}, write_buffer_water_mark_low = {}, write_buffer_water_mark_high = {}",
                serverProperties.isTcpNoDelay(), GlobalRedisProxyEnv.isServerTcpQuickAckEnable(), serverProperties.getWriteBufferWaterMarkLow(), serverProperties.getWriteBufferWaterMarkHigh());
        logger.info("CamelliaRedisProxyServer start at port: {}", port);
        GlobalRedisProxyEnv.setPort(port);
        GlobalRedisProxyEnv.getProxyShutdown().setServerChannelFuture(future);
        this.port = port;
        if (serverProperties.isClusterModeEnable()) {
            int cport = serverProperties.getCport();
            if (cport <= 0) {
                cport = port + 10000;
            }
            ChannelFuture channelFuture = serverBootstrap.bind(cport).sync();
            GlobalRedisProxyEnv.setCport(cport);
            GlobalRedisProxyEnv.getProxyShutdown().setCportChannelFuture(channelFuture);
            logger.info("CamelliaRedisProxyServer start in cluster mode at cport: {}", cport);
        }
        logger.info("CamelliaRedisProxyServer start success, version = {}", ProxyInfoUtils.VERSION);
        GlobalRedisProxyEnv.invokeStartOkCallback();
    }

    public int getPort() {
        return port;
    }
}