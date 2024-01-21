package com.netease.nim.camellia.redis.proxy.http;

import com.netease.nim.camellia.redis.proxy.monitor.CommandFailMonitor;
import com.netease.nim.camellia.redis.proxy.monitor.ProxyMonitorCollector;
import com.netease.nim.camellia.redis.proxy.netty.ChannelInfo;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpHeaderValues.KEEP_ALIVE;
import static io.netty.handler.codec.http.HttpHeaderValues.*;

/**
 * Created by caojiajun on 2024/1/19
 */
public class HttpCommandTaskQueue {

    private static final Logger logger = LoggerFactory.getLogger(HttpCommandTaskQueue.class);
    private final ChannelHandlerContext ctx;
    private final AtomicLong id = new AtomicLong(0);
    private final ChannelInfo channelInfo;
    private final Queue<HttpCommandTask> queue = new LinkedBlockingQueue<>(10240);
    private final AtomicBoolean callbacking = new AtomicBoolean(false);

    public HttpCommandTaskQueue(ChannelInfo channelInfo) {
        this.ctx = channelInfo.getCtx();
        this.channelInfo = channelInfo;
    }

    public ChannelInfo getChannelInfo() {
        return channelInfo;
    }

    public boolean add(HttpCommandTask task) {
        boolean offer = queue.offer(task);
        if (!offer) {
            logger.warn("HttpCommandTaskQueue full, consid = {}", channelInfo.getConsid());
        }
        return offer;
    }

    public void callback() {
        if (callbacking.compareAndSet(false, true)) {
            try {
                if (queue.isEmpty()) {
                    return;
                }
                do {
                    HttpCommandTask task = queue.peek();
                    HttpCommandTaskResponse response = task.getResponse();
                    if (response != null) {
                        reply(response);
                        queue.poll();
                    } else {
                        break;
                    }
                } while (!queue.isEmpty());
            } finally {
                callbacking.compareAndSet(true, false);
            }
        }
    }

    public void failMonitor(String reason) {
        if (ProxyMonitorCollector.isMonitorEnable()) {
            CommandFailMonitor.incr(reason);
        }
        ErrorLogCollector.collect(HttpCommandTaskQueue.class, "http api error, reason = " + reason + ", remote.ip=" + ctx.channel().remoteAddress());
    }

    private void reply(HttpCommandTaskResponse response) {
        HttpResponse httpResponse = response.getHttpResponse();
        if (response.isKeepalive()) {
            httpResponse.headers().set(CONNECTION, KEEP_ALIVE);
        } else {
            httpResponse.headers().set(CONNECTION, CLOSE);
        }
        httpResponse.headers().set(TRANSFER_ENCODING, CHUNKED);
        httpResponse.headers().set(CONTENT_TYPE, APPLICATION_JSON);
        ChannelFuture f = ctx.writeAndFlush(new HttpResponsePack(httpResponse, id.incrementAndGet()));
        if (!response.isKeepalive()) {
            f.addListener(ChannelFutureListener.CLOSE);
        }
        if (httpResponse.status().code() != 200) {
            failMonitor("httpCode=" + httpResponse.status().code());
        }
    }
}
