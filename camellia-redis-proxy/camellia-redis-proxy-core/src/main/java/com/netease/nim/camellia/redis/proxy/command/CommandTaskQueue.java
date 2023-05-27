package com.netease.nim.camellia.redis.proxy.command;

import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.netty.ChannelInfo;
import com.netease.nim.camellia.redis.proxy.netty.GlobalRedisProxyEnv;
import com.netease.nim.camellia.redis.proxy.plugin.ProxyPlugin;
import com.netease.nim.camellia.redis.proxy.plugin.ProxyPluginResponse;
import com.netease.nim.camellia.redis.proxy.plugin.ProxyReply;
import com.netease.nim.camellia.redis.proxy.reply.*;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnection;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * Created by caojiajun on 2019/12/12.
 */
public class CommandTaskQueue {

    private static final Logger logger = LoggerFactory.getLogger(CommandTaskQueue.class);

    private final ChannelInfo channelInfo;
    private final Queue<CommandTask> queue = GlobalRedisProxyEnv.getQueueFactory().generateCommandTaskQueue();
    private final AtomicBoolean callbacking = new AtomicBoolean(false);
    private final AtomicLong id = new AtomicLong(0);

    private List<ProxyPlugin> plugins;

    public CommandTaskQueue(ChannelInfo channelInfo) {
        this.channelInfo = channelInfo;
    }

    public ChannelInfo getChannelInfo() {
        return channelInfo;
    }

    public boolean add(CommandTask task) {
        if (channelInfo.isInSubscribe()) {
            return true;
        }
        plugins = task.getPlugins();
        boolean offer = queue.offer(task);
        if (!offer) {
            logger.warn("CommandTaskQueue full, consid = {}", channelInfo.getConsid());
        }
        return offer;
    }

    public void clear() {
        queue.clear();
    }

    public void callback() {
        if (callbacking.compareAndSet(false, true)) {
            try {
                if (queue.isEmpty()) {
                    return;
                }
                ChannelFuture future = null;
                do {
                    CommandTask task = queue.peek();
                    Reply reply = task.getReply();
                    if (reply != null) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("CommandTaskQueue callback, command = {}, reply = {}, consid = {}",
                                    task.getCommand() == null ? null : task.getCommand().getName(),
                                    reply.getClass().getSimpleName(), channelInfo.getConsid());
                        }
                        future = channelInfo.getCtx().writeAndFlush(new ReplyPack(reply, id.incrementAndGet()));
                        queue.poll();
                    } else {
                        break;
                    }
                } while (!queue.isEmpty());
                //check subscribe
                if (channelInfo.isInSubscribe()) {
                    RedisConnection bindConnection = channelInfo.getBindConnection();
                    if (bindConnection == null || !bindConnection.isValid()) {
                        if (future == null) {
                            logger.warn("client connection in subscribe status forced disconnect" +
                                            " because bind redis connection is null or invalid, bindConnection = {}, consid = {} in task queue without future",
                                    bindConnection == null ? null : bindConnection.getConnectionName(), channelInfo.getConsid());
                            channelInfo.getCtx().close();
                        } else {
                            future.addListener((ChannelFutureListener) channelFuture -> {
                                logger.warn("client connection in subscribe status forced disconnect" +
                                                " because bind redis connection is null or invalid, bindConnection = {}, consid = {} in task queue with future",
                                        bindConnection == null ? null : bindConnection.getConnectionName(), channelInfo.getConsid());
                                channelInfo.getCtx().close();
                            });
                        }
                    }
                }
            } finally {
                callbacking.compareAndSet(true, false);
            }
        }
    }

    public void reply(RedisCommand redisCommand, Reply reply, boolean fromPlugin) {
        if (!channelInfo.isInSubscribe()) {
            return;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("CommandTaskQueue reply, reply = {}, consid = {}",
                    reply.getClass().getSimpleName(), channelInfo.getConsid());
        }
        if (plugins != null && !plugins.isEmpty()) {
            CommandContext commandContext = new CommandContext(channelInfo.getBid(), channelInfo.getBgroup(), channelInfo.getClientSocketAddress());
            ProxyReply proxyReply = new ProxyReply(commandContext, redisCommand, reply, fromPlugin);
            for (ProxyPlugin plugin : plugins) {
                try {
                    ProxyPluginResponse response = plugin.executeReply(proxyReply);
                    if (!response.isPass()) {
                        channelInfo.getCtx().writeAndFlush(new ReplyPack(response.getReply(), id.incrementAndGet()));
                        return;
                    }
                } catch (Exception e) {
                    ErrorLogCollector.collect(CommandTaskQueue.class, "executeReply error", e);
                }
            }
        }

        ChannelFuture future = channelInfo.getCtx().writeAndFlush(new ReplyPack(reply, id.incrementAndGet()));

        if (reply instanceof ErrorReply) {
            RedisConnection bindConnection = channelInfo.getBindConnection();
            if (bindConnection != null && !bindConnection.isValid()) {
                future.addListener((ChannelFutureListener) channelFuture -> {
                    channelInfo.getCtx().close();
                    logger.warn("client connection in subscribe status forced disconnect" +
                                    " because bind redis connection is null or invalid, bindConnection = {}, consid = {} in direct reply case",
                            bindConnection.getConnectionName(), channelInfo.getConsid());
                });
            }
        }
    }

}
