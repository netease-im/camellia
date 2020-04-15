package com.netease.nim.camellia.redis.proxy.command.async;

import com.netease.nim.camellia.redis.proxy.reply.Reply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * Created by caojiajun on 2019/12/12.
 */
public class AsyncTask {

    private static final Logger logger = LoggerFactory.getLogger(AsyncTask.class);

    private AsyncTaskQueue taskQueue;
    private Reply reply;

    public AsyncTask(AsyncTaskQueue taskQueue) {
        this.taskQueue = taskQueue;
    }

    public void replyCompleted(Reply reply) {
        if (logger.isDebugEnabled()) {
            logger.debug("AsyncTask replyCompleted, reply = {}, consid = {}", reply.getClass().getSimpleName(), taskQueue.getChannelInfo().getConsid());
        }
        this.reply = reply;
        taskQueue.callback();
    }

    public Reply getReply() {
        return reply;
    }
}
