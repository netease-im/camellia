package com.netease.nim.camellia.redis.proxy.command.async;

import com.netease.nim.camellia.redis.proxy.command.async.converter.Converters;
import com.netease.nim.camellia.redis.proxy.netty.ChannelInfo;
import com.netease.nim.camellia.redis.proxy.reply.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * Created by caojiajun on 2019/12/12.
 */
public class AsyncTaskQueue {

    private static final Logger logger = LoggerFactory.getLogger(AsyncTaskQueue.class);

    private final ChannelInfo channelInfo;
    private final Queue<AsyncTask> queue = new LinkedBlockingQueue<>(1024*32);
    private final AtomicBoolean callbacking = new AtomicBoolean(false);
    private final AtomicLong id = new AtomicLong(0);
    private Converters converters;

    public AsyncTaskQueue(ChannelInfo channelInfo) {
        this.channelInfo = channelInfo;
    }

    public Converters getConverters() {
        return converters;
    }

    public void setConverters(Converters converters) {
        this.converters = converters;
    }

    public ChannelInfo getChannelInfo() {
        return channelInfo;
    }

    public boolean add(AsyncTask task) {
        if (channelInfo.isInSubscribe()) {
            return true;
        }
        boolean offer = queue.offer(task);
        if (!offer) {
            logger.warn("AsyncTaskQueue full, consid = {}", channelInfo.getConsid());
        }
        return offer;
    }

    public int size() {
        return queue.size();
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
                do {
                    AsyncTask task = queue.peek();
                    Reply reply = task.getReply();
                    if (reply != null) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("AsyncTaskQueue callback, command = {}, reply = {}, consid = {}",
                                    task.getCommand() == null ? null : task.getCommand().getName(),
                                    reply.getClass().getSimpleName(), channelInfo.getConsid());
                        }
                        channelInfo.getCtx().writeAndFlush(new ReplyPack(reply, id.incrementAndGet()));
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

    public void reply(Reply reply) {
        if (!channelInfo.isInSubscribe()) {
            return;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("AsyncTaskQueue reply, reply = {}, consid = {}",
                    reply.getClass().getSimpleName(), channelInfo.getConsid());
        }
        channelInfo.getCtx().writeAndFlush(new ReplyPack(reply, id.incrementAndGet()));
    }

}
