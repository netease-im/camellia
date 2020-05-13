package com.netease.nim.camellia.redis.proxy.command.async;

import com.netease.nim.camellia.core.util.CamelliaThreadFactory;
import com.netease.nim.camellia.core.util.SysUtils;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import io.netty.util.concurrent.EventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 *
 * Created by caojiajun on 2019/12/12.
 */
public class AsyncTask {

    private static final Logger logger = LoggerFactory.getLogger(AsyncTask.class);

    private static final ExecutorService asyncTaskExec = new ThreadPoolExecutor(SysUtils.getCpuNum(),
            SysUtils.getCpuNum(), 0, TimeUnit.SECONDS, new LinkedBlockingQueue<>(1000000), new CamelliaThreadFactory(AsyncTask.class));

    private final AsyncTaskQueue taskQueue;
    private Reply reply;

    public AsyncTask(AsyncTaskQueue taskQueue) {
        this.taskQueue = taskQueue;
    }

    public void replyCompleted(Reply reply) {
        if (logger.isDebugEnabled()) {
            logger.debug("AsyncTask replyCompleted, reply = {}, consid = {}", reply.getClass().getSimpleName(), taskQueue.getChannelInfo().getConsid());
        }
        this.reply = reply;
        EventExecutor executor = taskQueue.getChannelInfo().getCtx().executor();
        if (executor.inEventLoop()) {
            //如果是在work线程中，则需要切换一下线程执行
            //否则writeAndFlush在work线程和非work线程的执行顺序将无法保证
            asyncTaskExec.submit(taskQueue::callback);
        } else {
            taskQueue.callback();
        }
    }

    public Reply getReply() {
        return reply;
    }
}
