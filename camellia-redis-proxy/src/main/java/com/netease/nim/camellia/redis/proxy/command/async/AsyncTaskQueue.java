package com.netease.nim.camellia.redis.proxy.command.async;

import com.netease.nim.camellia.redis.proxy.netty.ChannelInfo;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 * Created by caojiajun on 2019/12/12.
 */
public class AsyncTaskQueue {

    private static final Logger logger = LoggerFactory.getLogger(AsyncTaskQueue.class);

    private final ChannelInfo channelInfo;
    private final LinkedBlockingQueue<AsyncTask> queue = new LinkedBlockingQueue<>(1000000);
    private final AtomicBoolean callbacking = new AtomicBoolean(false);

    public AsyncTaskQueue(ChannelInfo channelInfo) {
        this.channelInfo = channelInfo;
    }

    public ChannelInfo getChannelInfo() {
        return channelInfo;
    }

    public boolean add(AsyncTask task) {
        boolean offer = queue.offer(task);
        if (!offer) {
            logger.warn("AsyncTaskQueue full, consid = {}", channelInfo.getConsid());
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
                do {
                    AsyncTask task = queue.peek();
                    Reply reply = task.getReply();
                    if (reply != null) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("AsyncTaskQueue callback, reply = {}, consid = {}",
                                    reply.getClass().getSimpleName(), channelInfo.getConsid());
                        }
                        channelInfo.getCtx().writeAndFlush(reply);
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
}
