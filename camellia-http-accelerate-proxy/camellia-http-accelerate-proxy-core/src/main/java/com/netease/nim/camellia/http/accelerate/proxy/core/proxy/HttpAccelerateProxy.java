package com.netease.nim.camellia.http.accelerate.proxy.core.proxy;

import com.netease.nim.camellia.http.accelerate.proxy.core.conf.DynamicConf;
import com.netease.nim.camellia.http.accelerate.proxy.core.constants.Constants;
import com.netease.nim.camellia.http.accelerate.proxy.core.context.*;
import com.netease.nim.camellia.http.accelerate.proxy.core.route.transport.ITransportRouter;
import com.netease.nim.camellia.http.accelerate.proxy.core.status.ServerStatus;
import com.netease.nim.camellia.http.accelerate.proxy.core.transport.ITransportClient;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static io.netty.handler.codec.http.HttpHeaderNames.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaderNames.TRANSFER_ENCODING;
import static io.netty.handler.codec.http.HttpHeaderValues.*;

/**
 * Created by caojiajun on 2023/7/6
 */
public class HttpAccelerateProxy implements IHttpAccelerateProxy {

    private static final Logger logger = LoggerFactory.getLogger(HttpAccelerateProxy.class);

    private final ITransportRouter router;
    private boolean started;

    public HttpAccelerateProxy(ITransportRouter router) {
        this.router = router;
    }

    @Override
    public void start() {
        String host = DynamicConf.getString("http.accelerate.proxy.host", "0.0.0.0");
        int port = DynamicConf.getInt("http.accelerate.proxy.port", 11800);
        if (port <= 0) {
            logger.warn("http accelerate proxy skip start");
            return;
        }
        try {
            int bossThread = DynamicConf.getInt("http.accelerate.proxy.boss.thread", 1);
            int workThread = DynamicConf.getInt("http.accelerate.proxy.work.thread", Runtime.getRuntime().availableProcessors());
            EventLoopGroup bossGroup = new NioEventLoopGroup(bossThread, new DefaultThreadFactory("http-accelerate-proxy-boss-group"));
            EventLoopGroup workerGroup = new NioEventLoopGroup(workThread, new DefaultThreadFactory("http-accelerate-proxy-work-group"));
            ServerBootstrap bootstrap = new ServerBootstrap();
            int soBacklog = DynamicConf.getInt("http.accelerate.proxy.so.backlog", 1024);
            int soSndBuf = DynamicConf.getInt("http.accelerate.proxy.so.sndbuf", 10 * 1024 * 1024);
            int soRcvBuf = DynamicConf.getInt("http.accelerate.proxy.so.rcvbuf", 10 * 1024 * 1024);
            boolean tcpNoDelay = DynamicConf.getBoolean("http.accelerate.proxy.tcp.nodelay", true);
            boolean soKeepalive = DynamicConf.getBoolean("http.accelerate.proxy.so.keepalive", true);

            int low = DynamicConf.getInt("http.accelerate.proxy.write.buffer.water.mark.low", 128*1024);
            int high = DynamicConf.getInt("http.accelerate.proxy.write.buffer.water.mark.high", 512*1024);

            int maxContentLength = DynamicConf.getInt("http.accelerate.proxy.max.content.length", 20*1024*1024);
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
                            pipeline.addLast(new HttpServerCodec());
                            pipeline.addLast(new HttpObjectAggregator(maxContentLength));
                            pipeline.addLast(new SimpleChannelInboundHandler<FullHttpRequest>() {
                                @Override
                                protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest httpRequest) {
                                    ServerStatus.updateLastUseTime();
                                    LogBean logBean = new LogBean();
                                    logBean.setHost(httpRequest.headers().get("Host"));
                                    logBean.setTraceId(UUID.randomUUID().toString().replace("-", ""));
                                    QueryStringDecoder queryStringDecoder = new QueryStringDecoder(httpRequest.uri());
                                    logBean.setPath(queryStringDecoder.path());
                                    logBean.setStartTime(System.currentTimeMillis());
                                    ProxyRequest proxyRequest = new ProxyRequest(httpRequest, logBean);
                                    ITransportClient client = router.select(proxyRequest);
                                    CompletableFuture<ProxyResponse> future;
                                    if (client == null) {
                                        future = new CompletableFuture<>();
                                        logBean.setErrorReason(ErrorReason.TRANSPORT_SERVER_ROUTE_FAIL);
                                        future.complete(new ProxyResponse(Constants.BAD_GATEWAY, logBean));
                                    } else {
                                        future = client.send(proxyRequest);
                                    }
                                    future.thenAccept(proxyResponse -> {
                                        try {
                                            FullHttpResponse response = proxyResponse.getResponse();
                                            proxyResponse.getLogBean().setCode(response.status().code());
                                            boolean keepAlive = HttpUtil.isKeepAlive(httpRequest);
                                            boolean close = false;
                                            if (keepAlive) {
                                                if (!httpRequest.protocolVersion().isKeepAliveDefault()) {
                                                    response.headers().set(CONNECTION, CLOSE);
                                                    close = true;
                                                } else {
                                                    response.headers().set(CONNECTION, KEEP_ALIVE);
                                                }
                                            } else {
                                                response.headers().set(CONNECTION, CLOSE);
                                                close = true;
                                            }
                                            response.headers().set(TRANSFER_ENCODING, CHUNKED);
                                            ChannelFuture f = ctx.writeAndFlush(response);
                                            if (close) {
                                                f.addListener(ChannelFutureListener.CLOSE);
                                            }
                                            try {
                                                int refCnt = httpRequest.refCnt();
                                                if (refCnt > 0) {
                                                    httpRequest.release(refCnt);
                                                }
                                            } catch (Exception e) {
                                                logger.error(e.getMessage(), e);
                                            }
                                        } finally {
                                            proxyResponse.getLogBean().setEndTime(System.currentTimeMillis());
                                            LoggerUtils.logging(proxyResponse.getLogBean());
                                        }
                                    });
                                }
                            });
                        }
                    });
            bootstrap.bind(host, port).sync();
            logger.info("http accelerate proxy start success, host = {}, port = {}", host, port);
            started = true;
        } catch (Exception e) {
            logger.error("http accelerate proxy start error, host = {}, port = {}", host, port, e);
            throw new IllegalStateException(e);
        }
    }

    @Override
    public boolean isStarted() {
        return started;
    }
}
