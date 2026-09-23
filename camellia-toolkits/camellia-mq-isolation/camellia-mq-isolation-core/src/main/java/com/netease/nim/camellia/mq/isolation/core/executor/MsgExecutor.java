package com.netease.nim.camellia.mq.isolation.core.executor;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.netease.nim.camellia.core.client.env.ThreadContextSwitchStrategy;
import com.netease.nim.camellia.mq.isolation.core.stats.MsgExecutorMonitor;
import com.netease.nim.camellia.mq.isolation.core.stats.model.ExecutorStats;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by caojiajun on 2024/2/6
 */
public class MsgExecutor {

    private final String name;
    private final int threads;
    private final double maxPermitPercent;
    private final MsgTaskExecutor executor;
    private final ThreadContextSwitchStrategy strategy;

    private final ConcurrentLinkedHashMap<String, Semaphore> semaphoreMap;

    public MsgExecutor(String name, int threads, double maxPermitPercent, ThreadContextSwitchStrategy strategy) {
        this(name, threads, maxPermitPercent, strategy, false);
    }

    public MsgExecutor(String name, int threads, double maxPermitPercent, ThreadContextSwitchStrategy strategy,
                       boolean virtualThreadEnable) {
        this.name = name;
        this.strategy = strategy;
        this.executor = new MsgTaskExecutor(name, threads, virtualThreadEnable);
        this.threads = threads;
        this.maxPermitPercent = maxPermitPercent;
        this.semaphoreMap = new ConcurrentLinkedHashMap.Builder<String, Semaphore>()
                .initialCapacity(threads * 50)
                .maximumWeightedCapacity(threads * 50L)
                .build();
        MsgExecutorMonitor.register(this);
    }

    public boolean submit(String bizId, boolean autoIsolation, Runnable runnable) {
        if (!autoIsolation) {
            return executor.execute(strategy.wrapperRunnable(runnable));
        }
        Semaphore semaphore = tryAcquire(bizId);
        if (semaphore == null) {
            return false;
        }
        AtomicBoolean released = new AtomicBoolean(false);
        Runnable task = () -> {
            try {
                runnable.run();
            } finally {
                releaseOnce(semaphore, released);
            }
        };
        boolean delivered;
        try {
            delivered = executor.execute(strategy.wrapperRunnable(task));
        } catch (Throwable e) {
            releaseOnce(semaphore, released);
            throw e;
        }
        if (!delivered) {
            // The task never ran, so the permit must be returned to keep the isolation quota accurate.
            releaseOnce(semaphore, released);
        }
        return delivered;
    }

    private static void releaseOnce(Semaphore semaphore, AtomicBoolean released) {
        if (released.compareAndSet(false, true)) {
            semaphore.release();
        }
    }

    public String getName() {
        return name;
    }

    public ExecutorStats getStats() {
        ExecutorStats executorStats = new ExecutorStats();
        executorStats.setName(name);
        executorStats.setThreads(threads);
        executorStats.setCurrentThreads(executor.getCurrentThreads());
        executorStats.setActiveThreads(executor.getActiveTasks());
        executorStats.setExecutorType(executor.getMode());
        return executorStats;
    }

    public void shutdown() {
        try {
            executor.shutdown();
        } finally {
            MsgExecutorMonitor.unregister(name);
        }
    }

    public void shutdown(long timeout, java.util.concurrent.TimeUnit unit) {
        try {
            executor.shutdown(timeout, unit);
        } finally {
            MsgExecutorMonitor.unregister(name);
        }
    }

    public static void validateVirtualThreadSupport() {
        VirtualThreadExecutorFactory.validate();
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
