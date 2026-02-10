package com.netease.nim.camellia.redis.proxy.netty;

import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.upstream.IUpstreamClient;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnectionConfig;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.epoll.EpollChannelOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Queue;
import java.util.concurrent.CompletableFuture;

public class ReplyHandler extends SimpleChannelInboundHandler<Reply> {

    private static final Logger logger = LoggerFactory.getLogger(ReplyHandler.class);

    private final IUpstreamClient upstreamClient;
    private final Queue<CompletableFuture<Reply>> queue;
    private final String connectionName;

    public ReplyHandler(RedisConnectionConfig config, Queue<CompletableFuture<Reply>> queue, String connectionName) {
        this.upstreamClient = config.getUpstreamClient();
        this.queue = queue;
        this.connectionName = connectionName;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Reply reply) {
        if (logger.isDebugEnabled()) {
            logger.debug("{} receive reply, type = {}", connectionName, reply.getClass().getSimpleName());
        }
        try {
            CompletableFuture<Reply> future = queue.poll();
            if (future != null) {
                future.complete(reply);
                try {
                    if (reply instanceof ErrorReply) {
                        if (upstreamClient != null) {
                            upstreamClient.renew();
                        }
                    }
                } catch (Exception e) {
                    logger.error("upstream client renew fail by {}", connectionName, e);
                }
            } else {
                String log = connectionName + " redis receive reply with null future";
                ErrorLogCollector.collect(ReplyHandler.class, log);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}