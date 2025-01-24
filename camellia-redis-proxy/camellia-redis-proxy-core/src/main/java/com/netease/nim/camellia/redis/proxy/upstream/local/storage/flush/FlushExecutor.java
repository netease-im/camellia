package com.netease.nim.camellia.redis.proxy.upstream.local.storage.flush;


import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by caojiajun on 2025/1/7
 */
public class FlushExecutor {

    private final ThreadPoolExecutor executor;

    public FlushExecutor(int poolSize, int queueSize) {
        this.executor = new ThreadPoolExecutor(poolSize, poolSize, 0, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(queueSize), new FlushThreadFactory("flush", false));
    }

    public int size() {
        return executor.getQueue().size();
    }

    public boolean isInFlushThread() {
        Thread thread = Thread.currentThread();
        return thread instanceof FlushThread;
    }

    public void submit(Runnable task) {
        if (isInFlushThread()) {
            task.run();
        } else {
            executor.submit(task);
        }
    }
}
