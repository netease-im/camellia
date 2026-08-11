package com.netease.nim.camellia.tools.executor;

import com.netease.nim.camellia.tools.base.DynamicValueGetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

/**
 * Created by caojiajun on 2026/8/10
 */
public class CamelliaHashedBatchExecutor<T> implements CamelliaExecutor {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaHashedBatchExecutor.class);

    public static final int defaultQueueCount = 32;
    public static final int defaultWorkerCount = Math.max(1, Runtime.getRuntime().availableProcessors());
    public static final int defaultQueueCapacity = 100000;
    public static final int defaultMaxBatchSize = 100;
    public static final int defaultMinBatchSize = 1;
    public static final long defaultMaxBatchWaitMillis = 0;
    public static final int defaultPerQueueMaxDrainSize = 16;
    public static final int defaultSkipWaitPendingThreshold = 10000;
    public static final long defaultIdleWaitMillis = 10;

    private final AtomicLong workerIdGen = new AtomicLong(1);

    private final String name;
    private final int queueCount;
    private final int workerCount;
    private final int maxBatchSize;
    private final int minBatchSize;
    private final long maxBatchWaitMillis;
    private final int perQueueMaxDrainSize;
    private final int skipWaitPendingThreshold;
    private final long idleWaitMillis;
    private final Runnable workThreadInitCallback;
    private final BatchHandler<T> batchHandler;
    private final DynamicValueGetter<RejectedExecutionHandler<T>> rejectedExecutionHandler;
    private final List<DynamicCapacityLinkedBlockingQueue<T>> queues;
    private final AtomicBoolean[] queueRunning;
    private final List<WorkThread> workThreads;
    private final AtomicBoolean shutdown = new AtomicBoolean(false);
    private final AtomicBoolean initOk = new AtomicBoolean(false);
    private final AtomicInteger queuedTaskCount = new AtomicInteger(0);
    private final AtomicInteger inflightTaskCount = new AtomicInteger(0);

    public CamelliaHashedBatchExecutor(String name, BatchHandler<T> batchHandler) {
        this(new CamelliaHashedBatchExecutorConfig<>(name, batchHandler));
    }

    public CamelliaHashedBatchExecutor(String name, int queueCount, int workerCount, int maxBatchSize,
                                       BatchHandler<T> batchHandler) {
        this(new CamelliaHashedBatchExecutorConfig<>(name, queueCount, workerCount, maxBatchSize, batchHandler));
    }

    public CamelliaHashedBatchExecutor(CamelliaHashedBatchExecutorConfig<T> config) {
        checkConfig(config);
        this.name = CamelliaExecutorMonitor.genExecutorName(config.getName());
        this.queueCount = config.getQueueCount();
        this.workerCount = config.getWorkerCount();
        this.maxBatchSize = config.getMaxBatchSize();
        this.minBatchSize = Math.min(config.getMinBatchSize(), maxBatchSize);
        this.maxBatchWaitMillis = config.getMaxBatchWaitMillis();
        this.perQueueMaxDrainSize = Math.min(config.getPerQueueMaxDrainSize(), maxBatchSize);
        this.skipWaitPendingThreshold = config.getSkipWaitPendingThreshold();
        this.idleWaitMillis = config.getIdleWaitMillis();
        this.workThreadInitCallback = config.getWorkThreadInitCallback();
        this.batchHandler = config.getBatchHandler();
        this.rejectedExecutionHandler = config.getRejectedExecutionHandler();
        this.queues = new ArrayList<>(queueCount);
        this.queueRunning = new AtomicBoolean[queueCount];
        this.workThreads = new ArrayList<>(workerCount);
        for (int i = 0; i < queueCount; i++) {
            queues.add(new DynamicCapacityLinkedBlockingQueue<>(config.getDynamicQueueCapacity()));
            queueRunning[i] = new AtomicBoolean(false);
        }
        CamelliaExecutorMonitor.register(this);
    }

    private void checkConfig(CamelliaHashedBatchExecutorConfig<T> config) {
        if (config == null) {
            throw new IllegalArgumentException("config is null");
        }
        if (config.getName() == null || config.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("name is empty");
        }
        if (config.getQueueCount() <= 0) {
            throw new IllegalArgumentException("queueCount <= 0");
        }
        if (config.getWorkerCount() <= 0) {
            throw new IllegalArgumentException("workerCount <= 0");
        }
        if (config.getWorkerCount() > config.getQueueCount()) {
            throw new IllegalArgumentException("workerCount > queueCount");
        }
        if (config.getDynamicQueueCapacity() == null) {
            throw new IllegalArgumentException("dynamicQueueCapacity is null");
        }
        if (config.getMaxBatchSize() <= 0) {
            throw new IllegalArgumentException("maxBatchSize <= 0");
        }
        if (config.getMinBatchSize() <= 0) {
            throw new IllegalArgumentException("minBatchSize <= 0");
        }
        if (config.getPerQueueMaxDrainSize() <= 0) {
            throw new IllegalArgumentException("perQueueMaxDrainSize <= 0");
        }
        if (config.getMaxBatchWaitMillis() < 0) {
            throw new IllegalArgumentException("maxBatchWaitMillis < 0");
        }
        if (config.getIdleWaitMillis() <= 0) {
            throw new IllegalArgumentException("idleWaitMillis <= 0");
        }
        if (config.getBatchHandler() == null) {
            throw new IllegalArgumentException("batchHandler is null");
        }
        if (config.getRejectedExecutionHandler() == null) {
            throw new IllegalArgumentException("rejectedExecutionHandler is null");
        }
    }

    private void initWorkThreads() {
        if (initOk.get()) return;
        synchronized (initOk) {
            if (initOk.get()) return;
            for (int i = 0; i < workerCount; i++) {
                WorkThread workThread = new WorkThread(i);
                workThreads.add(workThread);
                workThread.start();
            }
            initOk.set(true);
        }
    }

    public boolean submit(Object hashKey, T task) {
        RejectedExecutionHandler<T> handler = rejectedExecutionHandler.get();
        return submit(hashKey, task, handler);
    }

    public boolean submit(Object hashKey, T task, RejectedExecutionHandler<T> handler) {
        if (task == null) {
            throw new NullPointerException("task is null");
        }
        if (shutdown.get()) {
            reject(hashKey, task, handler);
            return false;
        }
        initWorkThreads();
        if (shutdown.get()) {
            reject(hashKey, task, handler);
            return false;
        }
        int index = index(hashKey);
        BlockingQueue<T> queue = queues.get(index);
        boolean success = queue.offer(task);
        if (success) {
            queuedTaskCount.incrementAndGet();
            return true;
        }
        reject(hashKey, task, handler);
        return false;
    }

    private void reject(Object hashKey, T task, RejectedExecutionHandler<T> handler) {
        if (handler != null) {
            handler.rejectedExecution(hashKey, task, this);
        }
    }

    private int index(Object hashKey) {
        int hash = hashKey == null ? 0 : hashKey.hashCode();
        return positiveHash(hash) % queueCount;
    }

    private int positiveHash(int hash) {
        return hash == Integer.MIN_VALUE ? 0 : Math.abs(hash);
    }

    @Override
    public String getName() {
        return name;
    }

    public int getQueueCount() {
        return queueCount;
    }

    public int getWorkerCount() {
        return workerCount;
    }

    public int getMaxBatchSize() {
        return maxBatchSize;
    }

    public int getMinBatchSize() {
        return minBatchSize;
    }

    public long getMaxBatchWaitMillis() {
        return maxBatchWaitMillis;
    }

    public int getPerQueueMaxDrainSize() {
        return perQueueMaxDrainSize;
    }

    public int getSkipWaitPendingThreshold() {
        return skipWaitPendingThreshold;
    }

    public long getIdleWaitMillis() {
        return idleWaitMillis;
    }

    public int getQueueSize() {
        return queuedTaskCount.get();
    }

    public int getInflightTaskCount() {
        return inflightTaskCount.get();
    }

    public int getOutstandingTaskCount() {
        return queuedTaskCount.get() + inflightTaskCount.get();
    }

    public int getQueueSize(int queueIndex) {
        if (queueIndex < 0 || queueIndex >= queueCount) {
            throw new IllegalArgumentException("queueIndex out of range");
        }
        return queues.get(queueIndex).size();
    }

    public int getActiveCount() {
        int activeCount = 0;
        for (WorkThread workThread : workThreads) {
            if (workThread.isActive()) {
                activeCount ++;
            }
        }
        return activeCount;
    }

    public long getCompletedTaskCount() {
        long completedTaskCount = 0;
        for (WorkThread workThread : workThreads) {
            completedTaskCount += workThread.getCompletedTaskCount();
        }
        return completedTaskCount;
    }

    public CamelliaExecutorStats getStats() {
        CamelliaExecutorStats stats = new CamelliaExecutorStats();
        if (initOk.get()) {
            stats.setActiveThread(getActiveCount());
            stats.setThread(workThreads.size());
            stats.setCompletedTaskCount(getCompletedTaskCount());
            stats.setPendingTask(getQueueSize());
        } else {
            stats.setActiveThread(0);
            stats.setThread(0);
            stats.setPendingTask(0);
            stats.setCompletedTaskCount(0);
        }
        return stats;
    }

    public void shutdown() {
        shutdown.set(true);
        for (WorkThread workThread : workThreads) {
            workThread.interrupt();
        }
    }

    public boolean isShutdown() {
        return shutdown.get();
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        long deadline = System.nanoTime() + unit.toNanos(timeout);
        for (WorkThread workThread : workThreads) {
            long nanos = deadline - System.nanoTime();
            if (nanos <= 0) {
                return false;
            }
            long millis = TimeUnit.NANOSECONDS.toMillis(nanos);
            if (millis <= 0) {
                return false;
            }
            workThread.join(millis);
            if (workThread.isAlive()) {
                return false;
            }
        }
        return true;
    }

    private class WorkThread extends Thread {

        private final AtomicBoolean active = new AtomicBoolean(false);
        private final AtomicLong completedTaskCount = new AtomicLong(0);
        private int cursor;

        public WorkThread(int index) {
            setName("camellia-hashed-batch-executor-" + name + "-" + workerIdGen.getAndIncrement());
            this.cursor = index % queueCount;
        }

        public boolean isActive() {
            return active.get();
        }

        public long getCompletedTaskCount() {
            return completedTaskCount.get();
        }

        @Override
        public void run() {
            if (workThreadInitCallback != null) {
                workThreadInitCallback.run();
            }
            while (!shutdown.get()) {
                Batch<T> batch = null;
                try {
                    batch = drainBatch();
                    if (batch.tasks.isEmpty()) {
                        release(batch.queueIndexes);
                        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(idleWaitMillis));
                        continue;
                    }
                    waitMoreIfNecessary(batch);
                    active.set(true);
                    inflightTaskCount.addAndGet(batch.tasks.size());
                    try {
                        batchHandler.handle(batch.tasks);
                        completedTaskCount.addAndGet(batch.tasks.size());
                    } finally {
                        inflightTaskCount.addAndGet(-batch.tasks.size());
                        active.set(false);
                    }
                } catch (Exception e) {
                    logger.error("camellia hashed batch executor error, name = {}", name, e);
                } finally {
                    if (batch != null) {
                        release(batch.queueIndexes);
                    }
                }
            }
            int size = getQueueSize();
            logger.warn("camellia hashed batch executor work thread shutdown, thread.name = {}, skip.task.size = {}", getName(), size);
        }

        private void waitMoreIfNecessary(Batch<T> batch) {
            if (batch.tasks.size() >= minBatchSize) return;
            if (maxBatchWaitMillis <= 0) return;
            if (skipWaitPendingThreshold > 0 && queuedTaskCount.get() >= skipWaitPendingThreshold) return;
            long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(maxBatchWaitMillis);
            while (!shutdown.get() && batch.tasks.size() < minBatchSize && System.nanoTime() < deadline) {
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(Math.min(idleWaitMillis, maxBatchWaitMillis)));
                drainTo(batch);
                if (skipWaitPendingThreshold > 0 && queuedTaskCount.get() >= skipWaitPendingThreshold) return;
            }
        }

        private Batch<T> drainBatch() {
            Batch<T> batch = new Batch<>(maxBatchSize);
            drainTo(batch);
            return batch;
        }

        private void drainTo(Batch<T> batch) {
            if (batch.tasks.size() >= maxBatchSize) return;
            int start = cursor;
            cursor = (cursor + 1) % queueCount;
            for (int i = 0; i < queueCount && batch.tasks.size() < maxBatchSize; i++) {
                int queueIndex = (start + i) % queueCount;
                if (!tryLockQueue(batch, queueIndex)) {
                    continue;
                }
                pollOne(batch, queueIndex);
            }
            boolean progress = true;
            while (progress && batch.tasks.size() < maxBatchSize) {
                progress = false;
                for (int i = 0; i < queueCount && batch.tasks.size() < maxBatchSize; i++) {
                    int queueIndex = (start + i) % queueCount;
                    if (!tryLockQueue(batch, queueIndex)) {
                        continue;
                    }
                    int count = poll(batch, queueIndex, perQueueMaxDrainSize);
                    if (count > 0) {
                        progress = true;
                    }
                }
            }
        }

        private boolean tryLockQueue(Batch<T> batch, int queueIndex) {
            if (batch.contains(queueIndex)) {
                return true;
            }
            if (queues.get(queueIndex).isEmpty()) {
                return false;
            }
            boolean success = queueRunning[queueIndex].compareAndSet(false, true);
            if (success) {
                batch.addQueueIndex(queueIndex);
            }
            return success;
        }

        private void pollOne(Batch<T> batch, int queueIndex) {
            T task = queues.get(queueIndex).poll();
            if (task != null) {
                queuedTaskCount.decrementAndGet();
                batch.tasks.add(task);
            }
        }

        private int poll(Batch<T> batch, int queueIndex, int maxSize) {
            int count = 0;
            while (count < maxSize && batch.tasks.size() < maxBatchSize) {
                T task = queues.get(queueIndex).poll();
                if (task == null) {
                    break;
                }
                queuedTaskCount.decrementAndGet();
                batch.tasks.add(task);
                count ++;
            }
            return count;
        }
    }

    private void release(List<Integer> queueIndexes) {
        for (Integer queueIndex : queueIndexes) {
            queueRunning[queueIndex].set(false);
        }
        queueIndexes.clear();
    }

    private static class Batch<T> {

        private final List<T> tasks;
        private final List<Integer> queueIndexes = new ArrayList<>();

        public Batch(int maxBatchSize) {
            this.tasks = new ArrayList<>(maxBatchSize);
        }

        public boolean contains(int queueIndex) {
            return queueIndexes.contains(queueIndex);
        }

        public void addQueueIndex(int queueIndex) {
            queueIndexes.add(queueIndex);
        }
    }

    public static interface BatchHandler<T> {
        void handle(List<T> tasks) throws Exception;
    }

    public static interface RejectedExecutionHandler<T> {
        void rejectedExecution(Object hashKey, T task, CamelliaHashedBatchExecutor<T> executor);
    }

    public static class AbortPolicy<T> implements RejectedExecutionHandler<T> {
        @Override
        public void rejectedExecution(Object hashKey, T task, CamelliaHashedBatchExecutor<T> executor) {
            throw new RejectedExecutionException("Task " + task + " rejected from " + executor.name + ", hashKey = " + hashKey);
        }
    }

    public static class DiscardPolicy<T> implements RejectedExecutionHandler<T> {
        @Override
        public void rejectedExecution(Object hashKey, T task, CamelliaHashedBatchExecutor<T> executor) {
            logger.warn("Task {} is discard from {}, hashKey = {}", task, executor.name, hashKey);
        }
    }

    public static class CallerRunsPolicy<T> implements RejectedExecutionHandler<T> {
        @Override
        public void rejectedExecution(Object hashKey, T task, CamelliaHashedBatchExecutor<T> executor) {
            List<T> list = new ArrayList<>(1);
            list.add(task);
            try {
                executor.batchHandler.handle(list);
            } catch (Exception e) {
                throw new RejectedExecutionException("Task " + task + " rejected and caller runs error from " + executor.name + ", hashKey = " + hashKey, e);
            }
        }
    }
}
