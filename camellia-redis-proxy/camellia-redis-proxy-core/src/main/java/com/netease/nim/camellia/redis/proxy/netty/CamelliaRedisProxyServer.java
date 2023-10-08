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
        ServerBootstrap serverBootstrap = new ServerBootstrap();
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
            serverBootstrap.childOption(EpollChannelOption.TCP_QUICKACK, Boolean.TRUE);
        }

        ChannelFuture future1 = null;
        ChannelFuture future2 = null;
        if (port > 0) {
            future1 = serverBootstrap.bind(port).sync();
        }
        if (tlsPort > 0 && tlsPort != port) {
            future2 = serverBootstrap.bind(tlsPort).sync();
        }
        logger.info("CamelliaRedisProxyServer, so_backlog = {}, so_sendbuf = {}, so_rcvbuf = {}, so_keepalive = {}",
                serverProperties.getSoBacklog(), serverProperties.getSoSndbuf(), serverProperties.getSoRcvbuf(), serverProperties.isSoKeepalive());
        logger.info("CamelliaRedisProxyServer, tcp_no_delay = {}, tcp_quick_ack = {}, write_buffer_water_mark_low = {}, write_buffer_water_mark_high = {}",
                serverProperties.isTcpNoDelay(), GlobalRedisProxyEnv.isServerTcpQuickAckEnable(), serverProperties.getWriteBufferWaterMarkLow(), serverProperties.getWriteBufferWaterMarkHigh());
        logger.info("CamelliaRedisProxyServer, proxy_protocol_enable = {}", proxyProtocolEnable);
        if (proxyProtocolEnable) {
            logger.info("CamelliaRedisProxyServer, proxy_protocol_ports = {}", proxyProtocolPorts);
        }
        if (port > 0 && port != tlsPort) {
            logger.info("CamelliaRedisProxyServer start at port: {}", port);
        }
        if (tlsPort > 0) {
            logger.info("CamelliaRedisProxyServer start at port: {} with tls", tlsPort);
        }

        GlobalRedisProxyEnv.setPort(port);
        GlobalRedisProxyEnv.setTlsPort(tlsPort);
        this.port = port;
        this.tlsPort = tlsPort;
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
        ChannelFuture future3 = startUds();
        GlobalRedisProxyEnv.getProxyShutdown().setServerChannelFuture(future1, future2, future3);
        logger.info("CamelliaRedisProxyServer start success, version = {}", ProxyInfoUtils.VERSION);
        GlobalRedisProxyEnv.invokeStartOkCallback();
    }

    private ChannelFuture startUds() throws Exception {
        String udsPath = serverProperties.getUdsPath();
        if (udsPath == null || udsPath.length() == 0) {
            logger.info("CamelliaRedisProxyServer uds disabled, skip start");
            return null;
        }
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        EventLoopGroup bossGroup = GlobalRedisProxyEnv.getUdsBossGroup();
        EventLoopGroup workGroup = GlobalRedisProxyEnv.getUdsWorkGroup();
        Class<? extends ServerChannel> serverUdsChannelClass = GlobalRedisProxyEnv.getServerUdsChannelClass();
        if (bossGroup == null || workGroup == null || serverUdsChannelClass == null) {
            logger.warn("CamelliaRedisProxyServer uds start failed because os not support");
            return null;
        }
        serverBootstrap.group(bossGroup, workGroup)
                .channel(serverUdsChannelClass)
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
        ChannelFuture future = serverBootstrap.bind(new DomainSocketAddress(udsPath)).sync();
        this.udsPath = udsPath;
        GlobalRedisProxyEnv.setUdsPath(udsPath);
        logger.info("CamelliaRedisProxyServer start uds at {}", udsPath);
        return future;
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