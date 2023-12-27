package com.netease.nim.camellia.redis.proxy.netty;

import com.netease.nim.camellia.redis.proxy.command.ICommandInvoker;
import com.netease.nim.camellia.redis.proxy.conf.CamelliaServerProperties;
import com.netease.nim.camellia.redis.proxy.conf.Constants;
import com.netease.nim.camellia.redis.proxy.info.ProxyInfoUtils;
import com.netease.nim.camellia.redis.proxy.tls.frontend.ProxyFrontendTlsProvider;
import com.netease.nim.camellia.redis.proxy.util.ConfigInitUtil;
import com.netease.nim.camellia.redis.proxy.util.SocketUtils;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.unix.DomainSocketAddress;
import io.netty.channel.unix.UnixChannel;
import io.netty.handler.codec.haproxy.HAProxyMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


/**
 *
 * Created by caojiajun on 2019/11/5.
 */
public class CamelliaRedisProxyServer {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaRedisProxyServer.class);

    private final CamelliaServerProperties serverProperties;
    private final ServerHandler serverHandler;
    private final InitHandler tcpInitHandler = new InitHandler(ChannelType.tcp);
    private final InitHandler udsInitHandler = new InitHandler(ChannelType.uds);
    private int port;
    private int tlsPort;
    private String udsPath;

    public CamelliaRedisProxyServer(CamelliaServerProperties serverProperties, ICommandInvoker invoker) {
        GlobalRedisProxyEnv.init(serverProperties);
        this.serverProperties = serverProperties;
        this.serverHandler = new ServerHandler(invoker);
        if (logger.isInfoEnabled()) {
            logger.info("CamelliaRedisProxyServer init, netty-transport-mode = {}, bossThread = {}, workThread = {}",
                    GlobalRedisProxyEnv.getNettyTransportMode(), GlobalRedisProxyEnv.getBossThread(), GlobalRedisProxyEnv.getWorkThread());
        }
    }

    public void start() throws Exception {
        ServerBootstrap bootstrap = new ServerBootstrap();
        final boolean sslEnable;
        ProxyFrontendTlsProvider proxyFrontendTlsProvider;
        int tlsPort = serverProperties.getTlsPort();
        if (tlsPort > 0) {
            proxyFrontendTlsProvider = ConfigInitUtil.initProxyFrontendTlsProvider(serverProperties);
            if (proxyFrontendTlsProvider == null) {
                throw new IllegalArgumentException(serverProperties.getProxyFrontendTlsProviderClassName() + " init fail");
            }
            sslEnable = proxyFrontendTlsProvider.init();
        } else {
            sslEnable = false;
            proxyFrontendTlsProvider = null;
        }
        int port = serverProperties.getPort();
        //如果设置为这个特殊的负数端口，则会随机选择一个可用的端口
        if (port == Constants.Server.serverPortRandSig) {
            port = SocketUtils.findRandomAvailablePort();
        }
        final boolean proxyProtocolEnable = serverProperties.isProxyProtocolEnable();
        Set<Integer> proxyProtocolPorts = ConfigInitUtil.proxyProtocolPorts(serverProperties, port, tlsPort);

        bootstrap.group(GlobalRedisProxyEnv.getBossGroup(), GlobalRedisProxyEnv.getWorkGroup())
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
                    public void initChannel(SocketChannel channel) {
                        ChannelPipeline pipeline = channel.pipeline();
                        //proxy protocol
                        if (proxyProtocolEnable && proxyProtocolPorts.contains(channel.localAddress().getPort())) {
                            pipeline.addLast(new HAProxyMessageDecoder());
                            pipeline.addLast(new HAProxySourceIpHandler());
                        }
                        //tls
                        if (sslEnable && channel.localAddress().getPort() == GlobalRedisProxyEnv.getTlsPort()) {
                            pipeline.addLast(proxyFrontendTlsProvider.createSslHandler());
                        }
                        //idle close
                        if (Utils.idleCloseHandlerEnable(serverProperties)) {
                            pipeline.addLast(new IdleCloseHandler(serverProperties.getReaderIdleTimeSeconds(),
                                    serverProperties.getWriterIdleTimeSeconds(), serverProperties.getAllIdleTimeSeconds()));
                        }
                        //command decoder
                        pipeline.addLast(new CommandDecoder(serverProperties.getCommandDecodeMaxBatchSize(), serverProperties.getCommandDecodeBufferInitializerSize()));
                        //reply encoder
                        pipeline.addLast(new ReplyEncoder());
                        //connect manager
                        pipeline.addLast(tcpInitHandler);
                        //command transponder
                        pipeline.addLast(serverHandler);
                    }
                });
        if (GlobalRedisProxyEnv.isServerTcpQuickAckEnable()) {
            bootstrap.childOption(EpollChannelOption.TCP_QUICKACK, Boolean.TRUE);
        }

        //log
        logger.info("CamelliaRedisProxyServer, so_backlog = {}, so_sendbuf = {}, so_rcvbuf = {}, so_keepalive = {}",
                serverProperties.getSoBacklog(), serverProperties.getSoSndbuf(), serverProperties.getSoRcvbuf(), serverProperties.isSoKeepalive());
        logger.info("CamelliaRedisProxyServer, tcp_no_delay = {}, tcp_quick_ack = {}, write_buffer_water_mark_low = {}, write_buffer_water_mark_high = {}",
                serverProperties.isTcpNoDelay(), GlobalRedisProxyEnv.isServerTcpQuickAckEnable(), serverProperties.getWriteBufferWaterMarkLow(), serverProperties.getWriteBufferWaterMarkHigh());
        logger.info("CamelliaRedisProxyServer, proxy_protocol_enable = {}", proxyProtocolEnable);
        if (proxyProtocolEnable) {
            logger.info("CamelliaRedisProxyServer, proxy_protocol_ports = {}", proxyProtocolPorts);
        }

        List<BindInfo> bindInfoList = new ArrayList<>();
        //port
        if (port > 0) {
            bindInfoList.add(new BindInfo(bootstrap, port, false, false, null));
        }
        //tls port
        if (tlsPort > 0 && tlsPort != port) {
            bindInfoList.add(new BindInfo(bootstrap, tlsPort, false, true, null));
        }
        this.port = port;
        this.tlsPort = tlsPort;
        GlobalRedisProxyEnv.setPort(port);
        GlobalRedisProxyEnv.setTlsPort(tlsPort);
        //cport
        int cport = serverProperties.getCport();
        if (serverProperties.isClusterModeEnable()) {
            if (cport <= 0) {
                cport = port + 10000;
            }
            GlobalRedisProxyEnv.setClusterModeEnable(true);
        } else if (serverProperties.isSentinelModeEnable()) {
            if (cport <= 0) {
                cport = port + 10000;
            }
            GlobalRedisProxyEnv.setSentinelModeEnable(true);
        }
        if (cport > 0) {
            bindInfoList.add(new BindInfo(bootstrap, cport, true, false, null));
            GlobalRedisProxyEnv.setCport(cport);
        }
        //uds
        BindInfo bindInfo = startUds();
        if (bindInfo != null) {
            bindInfoList.add(bindInfo);
        }

        //before callback
        GlobalRedisProxyEnv.invokeBeforeStartCallback();
        //bind
        for (BindInfo info : bindInfoList) {
            if (info.port > 0 && !info.cport && !info.tls && info.udsPath == null) {
                logger.info("CamelliaRedisProxyServer start at port: {}", port);
                ChannelFuture future = info.bootstrap.bind(info.port).sync();
                GlobalRedisProxyEnv.getProxyShutdown().addServerFuture(future);
            } else if (info.port > 0 && !info.cport && info.tls && info.udsPath == null) {
                ChannelFuture future = info.bootstrap.bind(info.port).sync();
                logger.info("CamelliaRedisProxyServer start at port: {} with tls", tlsPort);
                GlobalRedisProxyEnv.getProxyShutdown().addServerFuture(future);
            } else if (info.port > 0 && info.cport && !info.tls && info.udsPath == null) {
                if (serverProperties.isClusterModeEnable()) {
                    logger.info("CamelliaRedisProxyServer start in cluster mode at cport: {}", info.port);
                } else if (serverProperties.isSentinelModeEnable()) {
                    logger.info("CamelliaRedisProxyServer start in sentinel mode at cport: {}", info.port);
                } else {
                    logger.info("CamelliaRedisProxyServer start at cport: {}", info.port);
                }
                ChannelFuture future = info.bootstrap.bind(info.port).sync();
                GlobalRedisProxyEnv.getProxyShutdown().setCportFuture(future);
            } else if (info.udsPath != null) {
                ChannelFuture future = info.bootstrap.bind(new DomainSocketAddress(udsPath)).sync();
                logger.info("CamelliaRedisProxyServer start at uds: {}", info.udsPath);
                GlobalRedisProxyEnv.getProxyShutdown().addServerFuture(future);
            }
        }
        //after callback
        GlobalRedisProxyEnv.invokeAfterStartCallback();
        logger.info("CamelliaRedisProxyServer start success, version = {}", ProxyInfoUtils.VERSION);
    }

    private BindInfo startUds() {
        String udsPath = serverProperties.getUdsPath();
        if (udsPath == null || udsPath.length() == 0) {
            logger.info("CamelliaRedisProxyServer uds disabled, skip start");
            return null;
        }
        ServerBootstrap bootstrap = new ServerBootstrap();
        EventLoopGroup bossGroup = GlobalRedisProxyEnv.getUdsBossGroup();
        EventLoopGroup workGroup = GlobalRedisProxyEnv.getUdsWorkGroup();
        Class<? extends ServerChannel> serverUdsChannelClass = GlobalRedisProxyEnv.getServerUdsChannelClass();
        if (bossGroup == null || workGroup == null || serverUdsChannelClass == null) {
            logger.warn("CamelliaRedisProxyServer uds start failed because os not support");
            return null;
        }
        bootstrap.group(bossGroup, workGroup)
                .channel(serverUdsChannelClass)
                .option(ChannelOption.SO_BACKLOG, serverProperties.getSoBacklog())
                .childOption(ChannelOption.SO_SNDBUF, serverProperties.getSoSndbuf())
                .childOption(ChannelOption.SO_RCVBUF, serverProperties.getSoRcvbuf())
                .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK,
                        new WriteBufferWaterMark(serverProperties.getWriteBufferWaterMarkLow(), serverProperties.getWriteBufferWaterMarkHigh()))
                .childHandler(new ChannelInitializer<UnixChannel>() {
                    @Override
                    public void initChannel(UnixChannel channel) {
                        ChannelPipeline pipeline = channel.pipeline();
                        //idle close
                        if (Utils.idleCloseHandlerEnable(serverProperties)) {
                            pipeline.addLast(new IdleCloseHandler(serverProperties.getReaderIdleTimeSeconds(),
                                    serverProperties.getWriterIdleTimeSeconds(), serverProperties.getAllIdleTimeSeconds()));
                        }
                        //command decoder
                        pipeline.addLast(new CommandDecoder(serverProperties.getCommandDecodeMaxBatchSize(), serverProperties.getCommandDecodeBufferInitializerSize()));
                        //reply encoder
                        pipeline.addLast(new ReplyEncoder());
                        //connect manager
                        pipeline.addLast(udsInitHandler);
                        //command transponder
                        pipeline.addLast(serverHandler);
                    }
                });
        this.udsPath = udsPath;
        GlobalRedisProxyEnv.setUdsPath(udsPath);
        return new BindInfo(bootstrap, -1, false, false, udsPath);
    }

    private static class BindInfo {
        ServerBootstrap bootstrap;
        int port;
        boolean tls;
        boolean cport;
        String udsPath;

        public BindInfo(ServerBootstrap bootstrap, int port, boolean cport, boolean tls, String udsPath) {
            this.bootstrap = bootstrap;
            this.port = port;
            this.cport = cport;
            this.tls = tls;
            this.udsPath = udsPath;
        }
    }

    public int getPort() {
        return port;
    }

    public int getTlsPort() {
        return tlsPort;
    }

    public String getUdsPath() {
        return udsPath;
    }
}