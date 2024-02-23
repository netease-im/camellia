package com.netease.nim.camellia.mq.isolation.core.executor;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.netease.nim.camellia.core.client.env.ThreadContextSwitchStrategy;
import com.netease.nim.camellia.mq.isolation.core.mq.TopicType;
import com.netease.nim.camellia.tools.executor.CamelliaThreadFactory;

import java.util.concurrent.Semaphore;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by caojiajun on 2024/2/6
 */
public class MsgExecutor {

    private final int threads;
    private final double maxPermitPercent;
    private final ThreadPoolExecutor executor;
    private final ThreadContextSwitchStrategy strategy;

    private final ConcurrentLinkedHashMap<String, Semaphore> semaphoreMap = new ConcurrentLinkedHashMap.Builder<String, Semaphore>()
            .initialCapacity(10000)
            .maximumWeightedCapacity(10000)
            .build();

    public MsgExecutor(String name, int threads, double maxPermitPercent, ThreadContextSwitchStrategy strategy) {
        this.strategy = strategy;
        this.executor = new ThreadPoolExecutor(threads, threads, 0, TimeUnit.SECONDS,
                new SynchronousQueue<>(), new CamelliaThreadFactory("camellia-mq-isolation[" + name + "]"), new ThreadPoolExecutor.CallerRunsPolicy());
        this.threads = threads;
        this.maxPermitPercent = maxPermitPercent;
    }

    public boolean submit(String bizId, boolean autoIsolation, Runnable runnable) {
        if (!autoIsolation) {
            executor.submit(strategy.wrapperRunnable(runnable));
            return true;
        }
        Semaphore semaphore = tryAcquire(bizId);
        if (semaphore == null) {
            return false;
        }
        executor.submit(strategy.wrapperRunnable(() -> {
            try {
                runnable.run();
            } finally {
                semaphore.release();
            }
        }));
        return true;
    }

    private Semaphore tryAcquire(String bizId) {
        Semaphore semaphore = semaphoreMap.get(bizId);
        if (semaphore == null) {
            semaphore = semaphoreMap.computeIfAbsent(bizId, k -> new Semaphore((int) (threads * maxPermitPercent)));
        }
        if (semaphore.tryAcquire()) {
            return semaphore;
        }
        return null;
    }
}
