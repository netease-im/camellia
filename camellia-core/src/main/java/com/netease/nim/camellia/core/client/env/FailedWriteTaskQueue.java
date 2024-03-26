package com.netease.nim.camellia.core.client.env;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by caojiajun on 2024/3/25
 */
public class FailedWriteTaskQueue {

    private static final Logger logger = LoggerFactory.getLogger(FailedWriteTaskQueue.class);

    private final BlockingQueue<FailedWriteTask> queue;

    public FailedWriteTaskQueue(int queueSize) {
        this.queue = new LinkedBlockingQueue<>(queueSize);
    }

    public void offerQueue(FailedWriteTask task) {
        boolean success = queue.offer(task);
        if (!success) {
            logger.warn("failed write task queue full, resource = {}, class = {}, method = {}, reason = {}, error = {}",
                    task.getResource().getUrl(), task.getClient().getClass().getName(),
                    task.getMethod().getName(), task.getFailedReason(), task.getError().toString());
        }
    }

    public FailedWriteTask popTask(long time, TimeUnit timeUnit) throws InterruptedException {
        return queue.poll(time, timeUnit);
    }
}
