package com.netease.nim.camellia.http.console;

import com.netease.nim.camellia.tools.executor.CamelliaHashedExecutor;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpHeaderValues.*;
import static io.netty.handler.codec.http.HttpHeaderValues.KEEP_ALIVE;
import static io.netty.handler.codec.http.HttpResponseStatus.*;

/**
 * Created by caojiajun on 2023/6/30
 */
public class CamelliaHttpConsoleServerHandler extends SimpleChannelInboundHandler<FullHttpRequest>  {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaHttpConsoleServerHandler.class);

    private final Map<String, ConsoleApiInvoker> invokerMap;
    private final CamelliaHashedExecutor executor;

    public CamelliaHttpConsoleServerHandler(Map<String, ConsoleApiInvoker> invokerMap, CamelliaHashedExecutor executor) {
        super();
        this.invokerMap = invokerMap;
        this.executor = executor;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
        try {
            String string = ctx.channel().remoteAddress().toString();
            executor.submit(string, () -> {
                try {
                    QueryStringDecoder queryStringDecoder = new QueryStringDecoder(request.uri());
                    String uri = queryStringDecoder.uri();
                    if (uri.contains("?")) {
                        uri = uri.split("\\?")[0];
                    }
                    ConsoleApiInvoker invoker = invokerMap.get(uri);
                    FullHttpResponse response;
                    if (invoker == null) {
                        response = new DefaultFullHttpResponse(request.protocolVersion(), NOT_FOUND,
                                Unpooled.wrappedBuffer(NOT_FOUND.reasonPhrase().getBytes(StandardCharsets.UTF_8)));
                    } else {
                        try {
                            ConsoleResult result = invoker.invoke(queryStringDecoder);
                            response = new DefaultFullHttpResponse(request.protocolVersion(), result.getCode(),
                                    Unpooled.wrappedBuffer(result.getData().getBytes(StandardCharsets.UTF_8)));
                        } catch (Exception e) {
                            logger.error("invoke error");
                            response = new DefaultFullHttpResponse(request.protocolVersion(), INTERNAL_SERVER_ERROR,
                                    Unpooled.wrappedBuffer(INTERNAL_SERVER_ERROR.reasonPhrase().getBytes(StandardCharsets.UTF_8)));
                        }
                    }
                    resp(ctx, request, response, false);
                } catch (Exception e) {
                    logger.error("error", e);
                    ctx.close();
                }
            });
        } catch (Exception e) {
            FullHttpResponse response = new DefaultFullHttpResponse(request.protocolVersion(), TOO_MANY_REQUESTS,
                        Unpooled.wrappedBuffer(TOO_MANY_REQUESTS.reasonPhrase().getBytes(StandardCharsets.UTF_8)));
            logger.error("too many requests", e);
            resp(ctx, request, response, true);
        }
    }

    private void resp(ChannelHandlerContext ctx, FullHttpRequest request, FullHttpResponse response, boolean forceDisconnect) {
        response.headers().set(CONTENT_TYPE, TEXT_PLAIN);
        response.headers().set(TRANSFER_ENCODING, CHUNKED);
        boolean keepAlive = HttpUtil.isKeepAlive(request);
        if (keepAlive) {
            if (!request.protocolVersion().isKeepAliveDefault()) {
                response.headers().set(CONNECTION, KEEP_ALIVE);
            }
        } else {
            response.headers().set(CONNECTION, CLOSE);
        }
        ChannelFuture f = ctx.writeAndFlush(response);
        if (!keepAlive || forceDisconnect) {
            f.addListener(ChannelFutureListener.CLOSE);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("console error", cause);
        ctx.close();
    }
}
