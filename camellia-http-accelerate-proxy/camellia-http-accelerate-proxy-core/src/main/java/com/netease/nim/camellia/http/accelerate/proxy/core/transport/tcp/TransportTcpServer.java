package com.netease.nim.camellia.http.accelerate.proxy.core.transport.tcp;

import com.netease.nim.camellia.http.accelerate.proxy.core.context.ErrorReason;
import com.netease.nim.camellia.http.accelerate.proxy.core.context.LoggerUtils;
import com.netease.nim.camellia.http.accelerate.proxy.core.transport.ITransportServer;
import com.netease.nim.camellia.http.accelerate.proxy.core.transport.tcp.codec.*;
import com.netease.nim.camellia.http.accelerate.proxy.core.conf.DynamicConf;
import com.netease.nim.camellia.http.accelerate.proxy.core.constants.Constants;
import com.netease.nim.camellia.http.accelerate.proxy.core.context.ProxyRequest;
import com.netease.nim.camellia.http.accelerate.proxy.core.context.ProxyResponse;
import com.netease.nim.camellia.http.accelerate.proxy.core.route.upstream.IUpstreamRouter;
import com.netease.nim.camellia.http.accelerate.proxy.core.status.ServerStatus;
import com.netease.nim.camellia.http.accelerate.proxy.core.upstream.IUpstreamClient;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Created by caojiajun on 2023/7/7
 */
public class TransportTcpServer implements ITransportServer {

    private static final Logger logger = LoggerFactory.getLogger(TransportTcpServer.class);

    private final IUpstreamRouter router;
    private boolean started = false;

    public TransportTcpServer(IUpstreamRouter router) {
        this.router = router;
    }

    @Override
    public void start() {
        int port = DynamicConf.getInt("transport.server.port", 11600);
        if (port <= 0) {
            logger.warn("transport tcp server skip start");
            return;
        }
        try {
            int bossThread = DynamicConf.getInt("transport.server.boss.thread", 1);
            int workThread = DynamicConf.getInt("transport.server.work.thread", 1);
            EventLoopGroup bossGroup = new NioEventLoopGroup(bossThread, new DefaultThreadFactory("sidecar-proxy-boss-group"));
            EventLoopGroup workerGroup = new NioEventLoopGroup(workThread, new DefaultThreadFactory("sidecar-proxy-work-group"));
            ServerBootstrap bootstrap = new ServerBootstrap();
            int soBacklog = DynamicConf.getInt("transport.server.so.backlog", 1024);
            int soSndBuf = DynamicConf.getInt("transport.server.so.sndbuf", 10 * 1024 * 1024);
            int soRcvBuf = DynamicConf.getInt("transport.server.so.rcvbuf", 10 * 1024 * 1024);
            boolean tcpNoDelay = DynamicConf.getBoolean("transport.server.tcp.no.delay", true);
            boolean soKeepalive = DynamicConf.getBoolean("transport.server.so.keep.alive", true);

            int low = DynamicConf.getInt("transport.server.write.buffer.water.mark.low", 128*1024);
            int high = DynamicConf.getInt("transport.server.write.buffer.water.mark.high", 512*1024);

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
                            pipeline.addLast(TcpPackDecoder.getName(), new TcpPackDecoder()); // IN
                            pipeline.addLast(new ChannelInboundHandlerAdapter() {
                                @Override
                                public void channelRead(ChannelHandlerContext ctx, Object msg) {
                                    try {
                                        if (msg instanceof TcpPack) {
                                            onTcpPack(ctx, (TcpPack) msg);
                                        } else {
                                            logger.warn("unknown pack");
                                        }
                                    } catch (Exception e) {
                                        logger.error("pack error", e);
                                    }
                                }
                            }); // IN
                        }
                    });
            bootstrap.bind(port).sync();
            logger.info("transport tcp server start success, port = {}", port);
            started = true;
        } catch (Exception e) {
            logger.error("transport tcp server start error, port = {}", port, e);
            throw new IllegalStateException(e);
        }
    }

    @Override
    public boolean isStarted() {
        return started;
    }

    private void onTcpPack(ChannelHandlerContext ctx, TcpPack pack) {
        TcpPackHeader header = pack.getHeader();
        if (header.getCmd() == TcpPackCmd.HEARTBEAT) {
            if (header.isAck()) {
                logger.warn("illegal heartbeat ack pack");
            } else {
                header.setAck();
                HeartbeatAckPack ackPack = new HeartbeatAckPack(ServerStatus.getStatus() == ServerStatus.Status.ONLINE);
                ctx.channel().writeAndFlush(TcpPack.newPack(header, ackPack).encode());
            }
        } else if (header.getCmd() == TcpPackCmd.REQUEST) {
            ServerStatus.updateLastUseTime();
            if (header.isAck()) {
                logger.warn("illegal request ack pack");
            } else {
                RequestPack requestPack = (RequestPack)pack.getBody();
                ProxyRequest proxyRequest = requestPack.getProxyRequest();
                proxyRequest.getLogBean().setTransportServerReceiveTime(System.currentTimeMillis());
                IUpstreamClient client = router.select(proxyRequest);
                CompletableFuture<ProxyResponse> future;
                if (client == null) {
                    future = new CompletableFuture<>();
                    proxyRequest.getLogBean().setErrorReason(ErrorReason.UPSTREAM_SERVER_ROUTE_FAIL);
                    future.complete(new ProxyResponse(Constants.BAD_GATEWAY, proxyRequest.getLogBean()));
                } else {
                    future = client.send(proxyRequest);
                }
                future.thenAccept(response -> {
                    try {
                        header.setAck();
                        ctx.channel().writeAndFlush(TcpPack.newPack(header, new RequestAckPack(response)).encode());
                    } finally {
                        LoggerUtils.logging(response.getLogBean());
                    }
                });
            }
        } else {
            logger.warn("unknown pack, seqId = {}", header.getSeqId());
        }
    }
}
