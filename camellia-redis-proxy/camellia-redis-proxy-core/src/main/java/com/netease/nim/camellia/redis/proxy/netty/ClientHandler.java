package com.netease.nim.camellia.redis.proxy.netty;

import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.upstream.client.RedisClientHub;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.epoll.EpollChannelOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Queue;
import java.util.concurrent.CompletableFuture;

public class ClientHandler extends SimpleChannelInboundHandler<Reply> {

    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);
    private final Queue<CompletableFuture<Reply>> queue;
    private final String clientName;

    public ClientHandler(Queue<CompletableFuture<Reply>> queue, String clientName) {
        this.queue = queue;
        this.clientName = clientName;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Reply msg) {
        if (logger.isDebugEnabled()) {
            logger.debug("{} receive reply, type = {}", clientName, msg.getClass().getSimpleName());
        }
        try {
            CompletableFuture<Reply> completableFuture = queue.poll();
            if (completableFuture != null) {
                completableFuture.complete(msg);
            } else {
                String log = clientName + " redis receive reply with null future";
                ErrorLogCollector.collect(ClientHandler.class, log);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            try {
                if (RedisClientHub.tcpQuickAck) {
                    ctx.channel().config().setOption(EpollChannelOption.TCP_QUICKACK, Boolean.TRUE);
                }
            } catch (Exception e) {
                ErrorLogCollector.collect(ClientHandler.class, "set TCP_QUICKACK error", e);
            }
        }
    }
}