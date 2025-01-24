package com.netease.nim.camellia.redis.proxy.upstream.local.storage.flush;


import com.netease.nim.camellia.redis.proxy.util.ExecutorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by caojiajun on 2025/1/7
 */
public class FlushExecutor {

    private static final Logger logger = LoggerFactory.getLogger(FlushExecutor.class);

    private final ThreadPoolExecutor executor;

    public FlushExecutor(int poolSize, int queueSize) {
        this.executor = new ThreadPoolExecutor(poolSize, poolSize, 0, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(queueSize), new FlushThreadFactory("flush", false));
        ExecutorUtils.scheduleAtFixedRate(() -> {
            int size = executor.getQueue().size();
            logger.info("flush executor, size = {}", size);
        }, 10, 10, TimeUnit.SECONDS);
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
