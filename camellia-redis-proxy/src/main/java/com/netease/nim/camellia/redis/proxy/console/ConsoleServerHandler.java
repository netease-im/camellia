package com.netease.nim.camellia.redis.proxy.console;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.netty.handler.codec.http.HttpHeaders.Names.*;
import static io.netty.handler.codec.http.HttpHeaders.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

@ChannelHandler.Sharable
public class ConsoleServerHandler extends SimpleChannelInboundHandler<Object> {

    private static final Logger logger = LoggerFactory.getLogger(ConsoleServerHandler.class);

    private HttpRequest request;
    private CustomRequestObject requestObject;
    private final ConsoleService consoleService;

    public ConsoleServerHandler(ConsoleService consoleService) {
        this.consoleService = consoleService;
    }

    private ConsoleResult handlerRequest(CustomRequestObject requestObject) {
        try {
            String uri = requestObject.getUriNoParam();
            if (uri.equalsIgnoreCase("/online")) {
                return consoleService.online();
            } else if (uri.equalsIgnoreCase("/offline")) {
                return consoleService.offline();
            } else if (uri.equalsIgnoreCase("/check")) {
                return consoleService.check();
            } else if (uri.equalsIgnoreCase("/status")) {
                return consoleService.status();
            } else if (uri.equalsIgnoreCase("/monitor")) {
                return consoleService.monitor();
            } else if (uri.equalsIgnoreCase("/reload")) {
                return consoleService.reload();
            } else if (uri.equalsIgnoreCase("/info")) {
                return consoleService.info(requestObject.getParams());
            } else if (uri.equalsIgnoreCase("/custom")) {
                return consoleService.custom(requestObject.getParams());
            } else if (uri.equalsIgnoreCase("/detect")) {
                return consoleService.detect(requestObject.getParams());
            }
            return ConsoleResult.error();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return ConsoleResult.error("unknown");
        }
    }

    private CustomRequestObject getRequestObject() {
        if (requestObject == null || requestObject.isContentComplete()) {
            requestObject = CustomRequestObject.getEmpty();
        }
        return requestObject;
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
        CustomRequestObject obj = getRequestObject();

        if (msg instanceof HttpRequest) {
            HttpRequest request = this.request = (HttpRequest) msg;

            if (is100ContinueExpected(request)) {
                send100Continue(ctx);
            }

            obj.setVersion(request.getProtocolVersion());
            obj.setMethod(request.getMethod());
            obj.setHost(getHost(request, "unknown"));
            SocketAddress addr = ctx.channel().remoteAddress();
            obj.setRemoteip(addr.toString());

            obj.setUri(request.getUri());
            obj.setHeaders(request.headers());
            QueryStringDecoder queryStringDecoder = new QueryStringDecoder(request.getUri());
            Map<String, List<String>> params = queryStringDecoder.parameters();
            obj.setParams(params);
            appendDecoderResult(obj, request);
        }

        if (msg instanceof HttpContent) {
            HttpContent httpContent = (HttpContent) msg;

            ByteBuf content = httpContent.content();
            if (content.isReadable()) {
                obj.setContent(content.toString(CharsetUtil.UTF_8));
                appendDecoderResult(obj, request);
            }
            if (msg instanceof LastHttpContent) {
                obj.setContentComplete(true);

                LastHttpContent trailer = (LastHttpContent) msg;
                if (!trailer.trailingHeaders().isEmpty()) {
                    obj.setTailHeaders(trailer.trailingHeaders());
                }
                writeResponse(obj, trailer, ctx);
            }
        }
    }

    private void writeResponse(CustomRequestObject requestObject, HttpObject currentObj, ChannelHandlerContext ctx){
        ConsoleResult consoleResult = handlerRequest(requestObject);
        if (consoleResult == null) {
            consoleResult = ConsoleResult.error();
        }

        boolean keepAlive = isKeepAlive(request);

        FullHttpResponse response = new DefaultFullHttpResponse(
                HTTP_1_1, currentObj.getDecoderResult().isSuccess()? OK : BAD_REQUEST,
                Unpooled.copiedBuffer(consoleResult.getData(), CharsetUtil.UTF_8));
        response.setStatus(consoleResult.getCode());
        response.headers().set(CONTENT_TYPE, "text/plain; charset=UTF-8");

        if (keepAlive) {
            response.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
        }
        response.headers().set(CONTENT_LENGTH, response.content().readableBytes());

        String cookieString = request.headers().get(COOKIE);
        if (cookieString != null) {
            Set<Cookie> cookies = CookieDecoder.decode(cookieString);
            if (!cookies.isEmpty()) {
                for (Cookie cookie: cookies) {
                    response.headers().add(SET_COOKIE, ServerCookieEncoder.encode(cookie));
                }
            }
        }

        ctx.write(response);
    }

    private static void appendDecoderResult(CustomRequestObject obj, HttpObject o) {
        DecoderResult result = o.getDecoderResult();
        if (!result.isSuccess()) {
            obj.setFailure(result.cause());
        }
    }

    private static void send100Continue(ChannelHandlerContext ctx) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, CONTINUE);
        ctx.write(response);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
    }
}
