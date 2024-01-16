package com.netease.nim.camellia.redis.proxy.http;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.redis.proxy.auth.ClientAuthProvider;
import com.netease.nim.camellia.redis.proxy.command.ICommandInvoker;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.netty.CamelliaRedisProxyServer;
import com.netease.nim.camellia.redis.proxy.netty.ChannelInfo;
import com.netease.nim.camellia.redis.proxy.netty.ChannelType;
import com.netease.nim.camellia.redis.proxy.upstream.IUpstreamClientTemplateFactory;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import static io.netty.handler.codec.http.HttpHeaderNames.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaderNames.TRANSFER_ENCODING;
import static io.netty.handler.codec.http.HttpHeaderValues.*;

/**
 * Created by caojiajun on 2024/1/16
 */
public class CamelliaRedisProxyHttpServer {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaRedisProxyHttpServer.class);

    public static final DefaultFullHttpResponse METHOD_NOT_ALLOWED = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.METHOD_NOT_ALLOWED);
    public static final DefaultFullHttpResponse NOT_FOUND = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND);
    public static final DefaultFullHttpResponse BAD_REQUEST = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST);
    public static final DefaultFullHttpResponse INTERNAL_SERVER_ERROR = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR);

    private final int port;
    private final HttpCommandInvoker invoker;

    public CamelliaRedisProxyHttpServer(int port, ICommandInvoker invoker) {
        this.port = port;
        this.invoker = new HttpCommandInvoker(invoker);
    }

    public CamelliaRedisProxyServer.BindInfo start() {
        try {
            if (port <= 0) {
                logger.info("CamelliaRedisProxyServer http disabled, skip start");
                return null;
            }
            int bossThread = ProxyDynamicConf.getInt("http.server.boss.thread", 1);
            int workThread = ProxyDynamicConf.getInt("http.server.work.thread", Runtime.getRuntime().availableProcessors());
            EventLoopGroup bossGroup = new NioEventLoopGroup(bossThread, new DefaultThreadFactory("camellia-http-boss-group"));
            EventLoopGroup workerGroup = new NioEventLoopGroup(workThread, new DefaultThreadFactory("camellia-http-work-group"));
            ServerBootstrap bootstrap = new ServerBootstrap();
            int soBacklog = ProxyDynamicConf.getInt("http.server.so.backlog", 1024);
            int soSndBuf = ProxyDynamicConf.getInt("http.server.so.sndbuf", 10 * 1024 * 1024);
            int soRcvBuf = ProxyDynamicConf.getInt("http.server.so.rcvbuf", 10 * 1024 * 1024);
            boolean tcpNoDelay = ProxyDynamicConf.getBoolean("http.server.tcp.nodelay", true);
            boolean soKeepalive = ProxyDynamicConf.getBoolean("http.server.so.keepalive", true);

            int low = ProxyDynamicConf.getInt("http.server.write.buffer.water.mark.low", 128*1024);
            int high = ProxyDynamicConf.getInt("http.server.write.buffer.water.mark.high", 512*1024);

            int maxContentLength = ProxyDynamicConf.getInt("http.server.max.content.length", 20*1024*1024);

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
                                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                                    logger.error(cause.getMessage(), cause);
                                    ctx.close();
                                }

                                @Override
                                protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest httpRequest) {
                                    try {
                                        HttpMethod method = httpRequest.method();
                                        if (method != HttpMethod.POST) {
                                            respResponse(ctx, httpRequest, METHOD_NOT_ALLOWED);
                                            return;
                                        }
                                        String uri = httpRequest.uri();
                                        if (!uri.equals("/commands")) {
                                            respResponse(ctx, httpRequest, NOT_FOUND);
                                            return;
                                        }
                                        ChannelInfo channelInfo = ChannelInfo.get(ctx);
                                        if (channelInfo == null) {
                                            channelInfo = ChannelInfo.init(ctx, ChannelType.http);
                                        }
                                        //request
                                        ByteBuf content = httpRequest.content();
                                        byte[] data = new byte[content.readableBytes()];
                                        content.readBytes(data);
                                        HttpCommandRequest request;
                                        try {
                                            request = JSONObject.parseObject(new String(data, StandardCharsets.UTF_8), HttpCommandRequest.class);
                                        } catch (Exception e) {
                                            respResponse(ctx, httpRequest, BAD_REQUEST);
                                            return;
                                        }
                                        //reply
                                        CompletableFuture<HttpCommandReply> future = invoker.invoke(channelInfo, request);
                                        future.thenAccept(reply -> {
                                            String string = JSONObject.toJSONString(reply);
                                            ByteBuf byteBuf = Unpooled.wrappedBuffer(string.getBytes(StandardCharsets.UTF_8));
                                            DefaultFullHttpResponse httpResponse = new DefaultFullHttpResponse(httpRequest.protocolVersion(),
                                                    HttpResponseStatus.OK, byteBuf);
                                            respResponse(ctx, httpRequest, httpResponse);
                                        });
                                    } catch (Exception e) {
                                        logger.error("internal error", e);
                                        respResponse(ctx, httpRequest, INTERNAL_SERVER_ERROR);
                                    }
                                }
                            });
                        }
                    });
            return new CamelliaRedisProxyServer.BindInfo(bootstrap, port, false, false, null, true);
        } catch (Exception e) {
            logger.error("camellia redis proxy http server start error, port = {}", port, e);
            throw new IllegalStateException(e);
        }
    }

    private void respResponse(ChannelHandlerContext ctx, FullHttpRequest httpRequest, FullHttpResponse httpResponse) {
        boolean keepAlive = HttpUtil.isKeepAlive(httpRequest);
        boolean close = false;
        if (keepAlive) {
            if (!httpRequest.protocolVersion().isKeepAliveDefault()) {
                httpResponse.headers().set(CONNECTION, CLOSE);
                close = true;
            } else {
                httpResponse.headers().set(CONNECTION, KEEP_ALIVE);
            }
        } else {
            httpResponse.headers().set(CONNECTION, CLOSE);
            close = true;
        }
        httpResponse.headers().set(TRANSFER_ENCODING, CHUNKED);
        ChannelFuture f = ctx.writeAndFlush(httpResponse);
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
    }
}
