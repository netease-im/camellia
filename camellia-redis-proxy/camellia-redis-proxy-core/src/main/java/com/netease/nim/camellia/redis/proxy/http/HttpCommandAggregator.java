package com.netease.nim.camellia.redis.proxy.http;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.redis.proxy.monitor.CommandFailMonitor;
import com.netease.nim.camellia.redis.proxy.monitor.ProxyMonitorCollector;
import com.netease.nim.camellia.redis.proxy.netty.ReplyEncoder;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.reply.ReplyPack;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpHeaderValues.*;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpHeaderValues.KEEP_ALIVE;

/**
 * Created by caojiajun on 2024/1/19
 */
public class HttpCommandAggregator {

    private final ChannelHandlerContext ctx;
    private final Queue<Request> queue = new LinkedBlockingQueue<>(1024);
    private final ReentrantLock lock = new ReentrantLock();
    private final List<Reply> replyList = new ArrayList<>();

    public HttpCommandAggregator(ChannelHandlerContext ctx) {
        this.ctx = ctx;
    }

    public boolean addRequest(FullHttpRequest request, HttpCommandRequest httpCommandRequest) {
        return queue.offer(new Request(request.protocolVersion(), isKeepAlive(request), httpCommandRequest));
    }

    public void addResponse(Object response) {
        lock.lock();
        try {
            Request request = queue.peek();
            if (request == null) {
                return;
            }
            if (request.httpCommandRequest == null) {
                if (response instanceof HttpResponse) {
                    reply((HttpResponse) response, request.keepAlive);
                    queue.poll();
                } else {
                    ctx.close();
                    failMonitor("IllegalResponse");
                }
            } else {
                if (response instanceof HttpCommandReply) {
                    if (!replyList.isEmpty()) {
                        ctx.close();
                        failMonitor("IllegalHttpCommandReply");
                        return;
                    }
                    HttpResponse httpResponse = toHttpResponse(request, (HttpCommandReply) response);
                    reply(httpResponse, request.keepAlive);
                    return;
                }
                Reply reply;
                if (response instanceof ReplyPack) {
                    reply = ((ReplyPack) response).getReply();
                } else if (response instanceof Reply) {
                    reply = (Reply) response;
                } else {
                    ctx.close();
                    failMonitor("IllegalReply");
                    return;
                }
                replyList.add(reply);
                if (request.httpCommandRequest.getCommands().size() == replyList.size()) {
                    List<Object> replies = HttpCommandReplyConverter.convert(replyList, request.httpCommandRequest.isReplyBase64());
                    HttpCommandReply httpCommandReply = new HttpCommandReply();
                    httpCommandReply.setRequestId(request.httpCommandRequest.getRequestId());
                    httpCommandReply.setCode(200);
                    httpCommandReply.setCommands(request.httpCommandRequest.getCommands());
                    httpCommandReply.setReplies(replies);

                    HttpResponse httpResponse = toHttpResponse(request, httpCommandReply);
                    reply(httpResponse, request.keepAlive);
                    replyList.clear();
                    queue.poll();
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public void failMonitor(String reason) {
        if (ProxyMonitorCollector.isMonitorEnable()) {
            CommandFailMonitor.incr(reason);
        }
        ErrorLogCollector.collect(HttpCommandAggregator.class, "http api error, reason = " + reason + ", remote.ip=" + ctx.channel().remoteAddress());
    }

    private HttpResponse toHttpResponse(Request request, HttpCommandReply httpCommandReply) {
        String string = JSONObject.toJSONString(httpCommandReply);
        ByteBuf byteBuf = Unpooled.wrappedBuffer(string.getBytes(StandardCharsets.UTF_8));
        return new DefaultFullHttpResponse(request.httpVersion, HttpResponseStatus.OK, byteBuf);
    }

    private void reply(HttpResponse httpResponse, boolean keepAlive) {
        if (keepAlive) {
            httpResponse.headers().set(CONNECTION, KEEP_ALIVE);
        } else {
            httpResponse.headers().set(CONNECTION, CLOSE);
        }
        httpResponse.headers().set(TRANSFER_ENCODING, CHUNKED);
        httpResponse.headers().set(CONTENT_TYPE, APPLICATION_JSON);
        ChannelFuture f = ctx.writeAndFlush(httpResponse);
        if (!keepAlive) {
            f.addListener(ChannelFutureListener.CLOSE);
        }
    }

    private static class Request {
        HttpVersion httpVersion;
        boolean keepAlive;
        HttpCommandRequest httpCommandRequest;

        public Request(HttpVersion httpVersion, boolean keepAlive, HttpCommandRequest httpCommandRequest) {
            this.httpVersion = httpVersion;
            this.keepAlive = keepAlive;
            this.httpCommandRequest = httpCommandRequest;
        }
    }

    private boolean isKeepAlive(HttpRequest httpRequest) {
        boolean keepAlive = HttpUtil.isKeepAlive(httpRequest);
        if (keepAlive) {
            return httpRequest.protocolVersion().isKeepAliveDefault();
        } else {
            return false;
        }
    }
}
