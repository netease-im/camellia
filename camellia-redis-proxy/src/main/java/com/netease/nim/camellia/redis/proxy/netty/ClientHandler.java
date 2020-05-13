package com.netease.nim.camellia.redis.proxy.netty;


import com.netease.nim.camellia.redis.proxy.reply.Reply;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;

public class ClientHandler extends SimpleChannelInboundHandler<Reply> {

    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);
    private final LinkedBlockingQueue<CompletableFuture<Reply>> queue;
    private final String clientName;

    public ClientHandler(LinkedBlockingQueue<CompletableFuture<Reply>> queue, String clientName) {
        this.queue = queue;
        this.clientName = clientName;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Reply msg) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("{} receive reply, type = {}", clientName, msg.getClass().getSimpleName());
        }
        try {
            CompletableFuture<Reply> completableFuture = queue.poll();
            completableFuture.complete(msg);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
