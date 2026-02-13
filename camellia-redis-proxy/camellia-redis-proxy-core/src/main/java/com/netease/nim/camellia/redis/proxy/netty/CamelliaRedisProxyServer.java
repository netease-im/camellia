package com.netease.nim.camellia.redis.proxy.netty;

import com.netease.nim.camellia.redis.proxy.command.CommandInvoker;
import com.netease.nim.camellia.redis.proxy.conf.*;
import com.netease.nim.camellia.redis.proxy.enums.ProxyMode;
import com.netease.nim.camellia.redis.proxy.http.CamelliaRedisProxyHttpServer;
import com.netease.nim.camellia.redis.proxy.info.ProxyInfoUtils;
import com.netease.nim.camellia.redis.proxy.tls.frontend.ProxyFrontendTlsProvider;
import com.netease.nim.camellia.redis.proxy.util.ConfigInitUtil;
import com.netease.nim.camellia.redis.proxy.util.SocketUtils;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.unix.DomainSocketAddress;
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

    private final InitHandler tcpInitHandler = new InitHandler(ChannelType.tcp);
    private int port;
    private int tlsPort;
    private String udsPath;
    private int httpPort;

    public void start() throws Exception {
        ServerBootstrap bootstrap = new ServerBootstrap();
        final boolean sslEnable;
        ProxyFrontendTlsProvider proxyFrontendTlsProvider;
        int tlsPort = ServerConf.tlsPort();
        if (tlsPort > 0) {
            proxyFrontendTlsProvider = ConfigInitUtil.initProxyFrontendTlsProvider();
            if (proxyFrontendTlsProvider == null) {
                throw new IllegalArgumentException("proxy frontend tls provider init fail");
            }
            sslEnable = proxyFrontendTlsProvider.init();
        } else {
            sslEnable = false;
            proxyFrontendTlsProvider = null;
        }
        int port = ServerConf.port();
        //如果设置为这个特殊的负数端口，则会随机选择一个可用的端口
        if (port == Constants.Server.serverPortRandSig) {
            port = SocketUtils.findRandomAvailablePort();
        }
        final boolean proxyProtocolEnable = ServerConf.isProxyProtocolEnable();
        Set<Integer> proxyProtocolPorts = ServerConf.proxyProtocolPorts(port, tlsPort);

        EventLoopGroupResult result = NettyConf.serverEventLoopGroup(NettyConf.Type.tcp_server);
        GlobalRedisProxyEnv.setTcpEventLoopGroupResult(result);

        CommandInvoker invoker = new CommandInvoker();
        ServerHandler serverHandler = new ServerHandler(invoker);

        bootstrap.group(result.bossGroup(), result.workGroup())
                .channel(result.serverChannelClass())
                .option(ChannelOption.SO_BACKLOG, NettyConf.soBacklog(NettyConf.Type.tcp_server))
                .childOption(ChannelOption.SO_SNDBUF, NettyConf.soSndbuf(NettyConf.Type.tcp_server))
                .childOption(ChannelOption.SO_RCVBUF, NettyConf.soRcvbuf(NettyConf.Type.tcp_server))
                .childOption(ChannelOption.TCP_NODELAY, NettyConf.tcpNoDelay(NettyConf.Type.tcp_server))
                .childOption(ChannelOption.SO_KEEPALIVE, NettyConf.soKeepalive(NettyConf.Type.tcp_server))
                .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK,
                        new WriteBufferWaterMark(NettyConf.writeBufferWaterMarkLow(NettyConf.Type.tcp_server),
                                NettyConf.writeBufferWaterMarkHigh(NettyConf.Type.tcp_server)))
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
                        if (Utils.idleCloseHandlerEnable()) {
                            pipeline.addLast(new IdleCloseHandler(NettyConf.readerIdleTimeSeconds(),
                                    NettyConf.writerIdleTimeSeconds(), NettyConf.allIdleTimeSeconds()));
                        }
                        //command decoder
                        pipeline.addLast(new CommandDecoder());
                        //reply encoder
                        if (ReplyBatchFlushUtils.enable()) {
                            pipeline.addLast(new ReplyFlushEncoder());
                            pipeline.addLast(new ReplyBufferEncoder());
                        } else {
                            pipeline.addLast(new ReplyEncoder());
                        }
                        //connect manager
                        pipeline.addLast(tcpInitHandler);
                        //command transponder
                        pipeline.addLast(serverHandler);
                    }
                });

        //log
        logger.info("CamelliaRedisProxyServer, boss_thread = {}, work_thread = {}", result.bossThread(), result.workThread());
        logger.info("CamelliaRedisProxyServer, so_backlog = {}, so_sendbuf = {}, so_rcvbuf = {}, so_keepalive = {}",
                NettyConf.soBacklog(NettyConf.Type.tcp_server), NettyConf.soSndbuf(NettyConf.Type.tcp_server), NettyConf.soRcvbuf(NettyConf.Type.tcp_server), NettyConf.soKeepalive(NettyConf.Type.tcp_server));
        logger.info("CamelliaRedisProxyServer, tcp_no_delay = {}, write_buffer_water_mark_low = {}, write_buffer_water_mark_high = {}",
                NettyConf.tcpNoDelay(NettyConf.Type.tcp_server), NettyConf.writeBufferWaterMarkLow(NettyConf.Type.tcp_server), NettyConf.writeBufferWaterMarkHigh(NettyConf.Type.tcp_server));
        logger.info("CamelliaRedisProxyServer, proxy_protocol_enable = {}", proxyProtocolEnable);
        if (proxyProtocolEnable) {
            logger.info("CamelliaRedisProxyServer, proxy_protocol_ports = {}", proxyProtocolPorts);
        }

        List<BindInfo> bindInfoList = new ArrayList<>();
        //port
        if (port > 0) {
            bindInfoList.add(new BindInfo(BindInfo.Type.PORT, bootstrap, port));
        }
        //tls port
        if (tlsPort > 0 && tlsPort != port) {
            bindInfoList.add(new BindInfo(BindInfo.Type.TLS_PORT, bootstrap, tlsPort));
        }
        this.port = port;
        this.tlsPort = tlsPort;
        GlobalRedisProxyEnv.setPort(port);
        GlobalRedisProxyEnv.setTlsPort(tlsPort);
        //cport
        int cport = ServerConf.cport();
        ProxyMode proxyMode = ServerConf.proxyMode();
        if (proxyMode == ProxyMode.cluster || proxyMode == ProxyMode.sentinel) {
            if (cport <= 0) {
                cport = port + 10000;
            }
        }
        if (cport > 0) {
            bindInfoList.add(new BindInfo(BindInfo.Type.CPORT, bootstrap, cport));
            GlobalRedisProxyEnv.setCport(cport);
        }
        //uds
        CamelliaRedisProxyUdsServer udsServer = new CamelliaRedisProxyUdsServer(serverHandler);
        BindInfo udsBindInfo = udsServer.start();
        if (udsBindInfo != null) {
            bindInfoList.add(udsBindInfo);
            this.udsPath = udsBindInfo.udsPath;
            GlobalRedisProxyEnv.setUdsPath(udsPath);
        }
        //http
        CamelliaRedisProxyHttpServer httpServer = new CamelliaRedisProxyHttpServer(invoker);
        BindInfo httpBindInfo = httpServer.start();
        if (httpBindInfo != null) {
            bindInfoList.add(httpBindInfo);
            this.httpPort = ServerConf.httpPort();
            GlobalRedisProxyEnv.setHttpPort(httpPort);
        }
        //before callback
        GlobalRedisProxyEnv.invokeBeforeStartCallback();
        //bind
        for (BindInfo info : bindInfoList) {
            if (info.port > 0 && info.type == BindInfo.Type.PORT) {
                logger.info("CamelliaRedisProxyServer start at port: {}", port);
                ChannelFuture future = info.bootstrap.bind(info.port).sync();
                GlobalRedisProxyEnv.getProxyShutdown().addServerFuture(future);
            } else if (info.port > 0 && info.type == BindInfo.Type.TLS_PORT) {
                ChannelFuture future = info.bootstrap.bind(info.port).sync();
                logger.info("CamelliaRedisProxyServer start at port: {} with tls", tlsPort);
                GlobalRedisProxyEnv.getProxyShutdown().addServerFuture(future);
            } else if (info.port > 0 && info.type == BindInfo.Type.CPORT) {
                if (proxyMode == ProxyMode.cluster) {
                    logger.info("CamelliaRedisProxyServer start in cluster mode at cport: {}", info.port);
                } else if (proxyMode == ProxyMode.sentinel) {
                    logger.info("CamelliaRedisProxyServer start in sentinel mode at cport: {}", info.port);
                } else {
                    logger.info("CamelliaRedisProxyServer start at cport: {}", info.port);
                }
                ChannelFuture future = info.bootstrap.bind(info.port).sync();
                GlobalRedisProxyEnv.getProxyShutdown().setCportFuture(future);
            } else if (info.udsPath != null && info.type == BindInfo.Type.UDS) {
                ChannelFuture future = info.bootstrap.bind(new DomainSocketAddress(udsPath)).sync();
                logger.info("CamelliaRedisProxyServer start at uds: {}", info.udsPath);
                GlobalRedisProxyEnv.getProxyShutdown().addServerFuture(future);
            } else if (info.port > 0 && info.type == BindInfo.Type.HTTP) {
                ChannelFuture future = info.bootstrap.bind(info.port).sync();
                logger.info("CamelliaRedisProxyServer start at http: {}", info.port);
                GlobalRedisProxyEnv.getProxyShutdown().addServerFuture(future);
            }
        }
        //after callback
        GlobalRedisProxyEnv.invokeAfterStartCallback();
        logger.info("CamelliaRedisProxyServer start success, version = {}", ProxyInfoUtils.VERSION);
    }

    public int getHttpPort() {
        return httpPort;
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