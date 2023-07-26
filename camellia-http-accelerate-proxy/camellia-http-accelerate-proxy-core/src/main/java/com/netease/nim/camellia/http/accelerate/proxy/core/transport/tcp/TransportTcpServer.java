package com.netease.nim.camellia.http.accelerate.proxy.core.transport.tcp;

import com.netease.nim.camellia.http.accelerate.proxy.core.status.ServerStartupStatus;
import com.netease.nim.camellia.http.accelerate.proxy.core.transport.AbstractTransportServer;
import com.netease.nim.camellia.http.accelerate.proxy.core.transport.codec.ProxyPack;
import com.netease.nim.camellia.http.accelerate.proxy.core.transport.codec.ProxyPackDecoder;
import com.netease.nim.camellia.http.accelerate.proxy.core.conf.DynamicConf;
import com.netease.nim.camellia.http.accelerate.proxy.core.route.upstream.IUpstreamRouter;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by caojiajun on 2023/7/7
 */
public class TransportTcpServer extends AbstractTransportServer {

    private static final Logger logger = LoggerFactory.getLogger(TransportTcpServer.class);

    private ServerStartupStatus status = ServerStartupStatus.INIT;

    public TransportTcpServer(IUpstreamRouter router) {
        super(router);
    }

    @Override
    public void start() {
        String host = DynamicConf.getString("transport.tcp.server.host", "0.0.0.0");
        int port = DynamicConf.getInt("transport.tcp.server.port", 11600);
        if (port <= 0) {
            logger.warn("transport tcp server skip start");
            status = ServerStartupStatus.SKIP;
            return;
        }
        try {
            int bossThread = DynamicConf.getInt("transport.tcp.server.boss.thread", 1);
            int workThread = DynamicConf.getInt("transport.tcp.server.work.thread", Runtime.getRuntime().availableProcessors());
            EventLoopGroup bossGroup = new NioEventLoopGroup(bossThread, new DefaultThreadFactory("transport-tcp-server-boss-group"));
            EventLoopGroup workerGroup = new NioEventLoopGroup(workThread, new DefaultThreadFactory("transport-tcp-server-work-group"));
            ServerBootstrap bootstrap = new ServerBootstrap();
            int soBacklog = DynamicConf.getInt("transport.tcp.server.so.backlog", 1024);
            int soSndBuf = DynamicConf.getInt("transport.tcp.server.so.sndbuf", 10 * 1024 * 1024);
            int soRcvBuf = DynamicConf.getInt("transport.tcp.server.so.rcvbuf", 10 * 1024 * 1024);
            boolean tcpNoDelay = DynamicConf.getBoolean("transport.tcp.server.tcp.no.delay", true);
            boolean soKeepalive = DynamicConf.getBoolean("transport.tcp.server.so.keep.alive", true);

            int low = DynamicConf.getInt("transport.tcp.server.write.buffer.water.mark.low", 128*1024);
            int high = DynamicConf.getInt("transport.tcp.server.write.buffer.water.mark.high", 512*1024);

            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, soBacklog)
                    .childOption(ChannelOption.SO_SNDBUF, soSndBuf)
                    .childOption(ChannelOption.SO_RCVBUF, soRcvBuf)
                    .childOption(ChannelOption.TCP_NODELAY, tcpNoDelay)
                    .childOption(ChannelOption.SO_KEEPALIVE, soKeepalive)
                    .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(low, high))
                    .childHandler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel channel) {
                            ChannelPipeline pipeline = channel.pipeline();
                            pipeline.addLast(ProxyPackDecoder.getName(), new ProxyPackDecoder());
                            pipeline.addLast(new ChannelInboundHandlerAdapter() {
                                @Override
                                public void channelRead(ChannelHandlerContext ctx, Object msg) {
                                    try {
                                        if (msg instanceof ProxyPack) {
                                            onProxyPack(ctx, (ProxyPack) msg);
                                        } else {
                                            logger.warn("unknown pack");
                                        }
                                    } catch (Exception e) {
                                        logger.error("pack error", e);
                                    }
                                }

                                @Override
                                public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                    super.channelActive(ctx);
                                    logger.info("new tcp client connection, channel = {}", ctx.channel());
                                }

                                @Override
                                public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                                    super.channelInactive(ctx);
                                    logger.info("tcp client connection disconnect, channel = {}", ctx.channel());
                                }
                            });
                        }
                    });
            bootstrap.bind(port).sync();
            logger.info("transport tcp server start success, host = {}, port = {}", host, port);
            status = ServerStartupStatus.SUCCESS;
        } catch (Exception e) {
            status = ServerStartupStatus.FAIL;
            logger.error("transport tcp server start error, host = {}, port = {}", host, port, e);
            throw new IllegalStateException(e);
        }
    }

    @Override
    public ServerStartupStatus getStatus() {
        return status;
    }
}
