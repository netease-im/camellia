package com.netease.nim.camellia.tools.executor;

import com.netease.nim.camellia.tools.base.DynamicValueGetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

/**
 * Created by caojiajun on 2026/10/9
 *
 * 按 hashKey 合并 + 时间窗批量提交的执行器。
 *
 * 和 CamelliaHashedBatchExecutor（流式批量）的区别：
 * 1、流式批量是"提交即入队，按批次取出执行"，合并/去重需要业务自己写在 BatchHandler 里；
 *    本执行器是"提交时按 hashKey 合并成槽位，窗口到期才交给 BatchHandler"，同一个 hashKey 在一个窗口内只出现一次；
 * 2、流式批量的批次由 drain 决定，去重效果受"同时非空队列数/批次上限"影响，吞吐上升时会退化；
 *    本执行器的窗口由时间保证，合并比只取决于"每 key 在窗口内的提交次数"，不会因为吞吐上升而被压缩；
 * 3、两者都是"hashKey 路由到固定队列 + 队列级别互斥"，都不保证任务落在固定线程上。
 */
public class CamelliaMergingExecutor<T> implements CamelliaExecutor {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaMergingExecutor.class);

    public static final int defaultQueueCount = 32;
    public static final int defaultWorkerCount = Math.max(1, Runtime.getRuntime().availableProcessors());
    public static final int defaultMaxBatchSize = 100;
    public static final int defaultSkipWaitPendingThreshold = 10000;
    public static final long defaultIdleWaitMillis = 10;
    public static final long defaultWindowMillis = 1000;
    public static final int defaultMaxAccumulateSize = 100000;

    private final AtomicLong workerIdGen = new AtomicLong(1);

    private final String name;
    private final int queueCount;
    private final int workerCount;
    private final int maxBatchSize;
    private final int skipWaitPendingThreshold;
    private final long idleWaitMillis;
    private final long windowMillis;
    private final int maxAccumulateSize;
    private final Runnable workThreadInitCallback;
    private final Merger<T> merger;
    private final BatchHandler<T> batchHandler;
    private final DynamicValueGetter<RejectedExecutionHandler<T>> rejectedExecutionHandler;
    private final List<WindowQueue<T>> windowQueues;
    private final AtomicLong[] queueNextFlushTime;
    private final AtomicBoolean[] queueRunning;
    private final List<WorkThread> workThreads;
    private final ThreadLocal<List<Integer>> handlingQueueIndexes = new ThreadLocal<>();
    private final AtomicBoolean shutdown = new AtomicBoolean(false);
    private final AtomicBoolean initOk = new AtomicBoolean(false);
    private final AtomicInteger inflightTaskCount = new AtomicInteger(0);

    private final RejectedExecutionHandler<T> defaultRejectedExecutionHandler = new CallerRunsPolicy<>();

    public CamelliaMergingExecutor(String name, Merger<T> merger, BatchHandler<T> batchHandler) {
        this(new CamelliaMergingExecutorConfig<>(name, merger, batchHandler));
    }

    public CamelliaMergingExecutor(String name, int queueCount, int workerCount, int maxBatchSize,
                                   long windowMillis, Merger<T> merger, BatchHandler<T> batchHandler) {
        this(new CamelliaMergingExecutorConfig<>(name, queueCount, workerCount, maxBatchSize,
                windowMillis, merger, batchHandler));
    }

    public CamelliaMergingExecutor(CamelliaMergingExecutorConfig<T> config) {
        checkConfig(config);
        this.name = CamelliaExecutorMonitor.genExecutorName(config.getName());
        this.queueCount = config.getQueueCount();
        this.workerCount = config.getWorkerCount();
        this.maxBatchSize = config.getMaxBatchSize();
        this.skipWaitPendingThreshold = config.getSkipWaitPendingThreshold();
        this.idleWaitMillis = config.getIdleWaitMillis();
        this.windowMillis = config.getWindowMillis();
        this.maxAccumulateSize = config.getMaxAccumulateSize();
        this.workThreadInitCallback = config.getWorkThreadInitCallback();
        this.merger = config.getMerger();
        this.batchHandler = config.getBatchHandler();
        this.rejectedExecutionHandler = config.getRejectedExecutionHandler();
        this.windowQueues = new ArrayList<>(queueCount);
        this.queueNextFlushTime = new AtomicLong[queueCount];
        this.queueRunning = new AtomicBoolean[queueCount];
        this.workThreads = new ArrayList<>(workerCount);
        for (int i = 0; i < queueCount; i++) {
            windowQueues.add(new WindowQueue<>());
            queueNextFlushTime[i] = new AtomicLong(0);
            queueRunning[i] = new AtomicBoolean(false);
        }
        CamelliaExecutorMonitor.register(this);
    }

    private void checkConfig(CamelliaMergingExecutorConfig<T> config) {
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
        if (config.getMaxBatchSize() <= 0) {
            throw new IllegalArgumentException("maxBatchSize <= 0");
        }
        if (config.getIdleWaitMillis() <= 0) {
            throw new IllegalArgumentException("idleWaitMillis <= 0");
        }
        if (config.getWindowMillis() <= 0) {
            throw new IllegalArgumentException("windowMillis <= 0");
        }
        if (config.getMaxAccumulateSize() <= 0) {
            throw new IllegalArgumentException("maxAccumulateSize <= 0");
        }
        if (config.getMerger() == null) {
            throw new IllegalArgumentException("merger is null");
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

    /**
     * 提交一个任务：按 hashKey 合并进槽位，同一个 key 在一个窗口内只保留合并后的一条
     *
     * 返回 false 表示"没有进窗口"，此时已经执行了拒绝策略（默认是在调用方线程同步执行）
     */
    public boolean submit(Object hashKey, T task, RejectedExecutionHandler<T> handler) {
        if (task == null) {
            throw new NullPointerException("task is null");
        }
        if (handler == null) {
            // 默认"同步执行"，避免未配置拒绝策略时静默丢数据
            handler = defaultRejectedExecutionHandler;
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
        long now = System.currentTimeMillis();
        AtomicLong nextFlushTime = queueNextFlushTime[index];
        if (nextFlushTime.get() == 0) {
            //首次提交才开窗，窗口是"每队列滚动窗口"
            nextFlushTime.set(now + windowMillis);
        }
        Map<Object, Slot<T>> slots = windowQueues.get(index).slots;
        int maxAccumulateSize = this.maxAccumulateSize;
        Slot<T> result = slots.compute(hashKey, (key, oldValue) -> {
            if (oldValue == null) {
                //新增 hashKey：槽位已达上限时拒绝，保证内存上界为 queueCount * maxAccumulateSize
                //同一个 hashKey 的重复提交只是合并、不新增槽位，任何时候都不会被拒绝
                if (slots.size() >= maxAccumulateSize) {
                    return null;
                }
                return new Slot<>(task);
            }
            return new Slot<>(merger.merge(oldValue.value, task));
        });
        if (result == null) {
            //槽位已满：催一次刷新，并把这条提交交给拒绝策略做背压
            nextFlushTime.set(now);
            reject(hashKey, task, handler);
            return false;
        }
        if (slots.size() >= maxAccumulateSize) {
            nextFlushTime.set(now);
        }
        return true;
    }

    private void reject(Object hashKey, T task, RejectedExecutionHandler<T> handler) {
        if (handler != null) {
            handler.rejectedExecution(hashKey, task, this);
        }
    }

    private void callerRuns(Object hashKey, T task) {
        int queueIndex = index(hashKey);
        List<Integer> currentQueueIndexes = handlingQueueIndexes.get();
        if (currentQueueIndexes != null && currentQueueIndexes.contains(queueIndex)) {
            handleCallerRunsTask(hashKey, task);
            return;
        }
        boolean interrupted = false;
        while (!queueRunning[queueIndex].compareAndSet(false, true)) {
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1));
            if (Thread.interrupted()) {
                interrupted = true;
            }
        }
        try {
            handleCallerRunsTask(hashKey, task);
        } finally {
            queueRunning[queueIndex].set(false);
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void handleCallerRunsTask(Object hashKey, T task) {
        List<T> tasks = new ArrayList<>(1);
        tasks.add(task);
        try {
            batchHandler.handle(tasks);
        } catch (Exception e) {
            throw new RejectedExecutionException("Task " + task + " rejected and caller runs error from " + name + ", hashKey = " + hashKey, e);
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

    public int getSkipWaitPendingThreshold() {
        return skipWaitPendingThreshold;
    }

    public long getIdleWaitMillis() {
        return idleWaitMillis;
    }

    public long getWindowMillis() {
        return windowMillis;
    }

    public int getMaxAccumulateSize() {
        return maxAccumulateSize;
    }

    /**
     * 待刷新的槽位数，也就是"下一个窗口将要处理的合并后条数"
     */
    public int getQueueSize() {
        int size = 0;
        for (int i = 0; i < queueCount; i++) {
            if (queueNextFlushTime[i].get() != 0) {
                size += windowQueues.get(i).slots.size();
            }
        }
        return size;
    }

    public int getQueueSize(int queueIndex) {
        if (queueIndex < 0 || queueIndex >= queueCount) {
            throw new IllegalArgumentException("queueIndex out of range");
        }
        return windowQueues.get(queueIndex).slots.size();
    }

    public int getInflightTaskCount() {
        return inflightTaskCount.get();
    }

    public int getOutstandingTaskCount() {
        return getQueueSize() + inflightTaskCount.get();
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

    private void release(List<Integer> queueIndexes) {
        for (Integer queueIndex : queueIndexes) {
            queueRunning[queueIndex].set(false);
        }
        queueIndexes.clear();
    }

    private class WorkThread extends Thread {

        private final AtomicBoolean active = new AtomicBoolean(false);
        private final AtomicLong completedTaskCount = new AtomicLong(0);
        private int cursor;

        public WorkThread(int index) {
            setName("camellia-merging-executor-" + name + "-" + workerIdGen.getAndIncrement());
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
                    long now = System.currentTimeMillis();
                    long nextDeadline = Long.MAX_VALUE;
                    long totalPending = 0;
                    if (skipWaitPendingThreshold > 0) {
                        for (int i = 0; i < queueCount; i++) {
                            if (queueNextFlushTime[i].get() != 0) {
                                totalPending += windowQueues.get(i).slots.size();
                            }
                        }
                    }
                    boolean backlogTrigger = skipWaitPendingThreshold > 0 && totalPending >= skipWaitPendingThreshold;
                    batch = new Batch<>(maxBatchSize);
                    int start = cursor;
                    cursor = (cursor + 1) % queueCount;
                    for (int i = 0; i < queueCount && batch.tasks.size() < maxBatchSize; i++) {
                        int queueIndex = (start + i) % queueCount;
                        AtomicLong nextFlushTime = queueNextFlushTime[queueIndex];
                        long flushTime = nextFlushTime.get();
                        if (flushTime == 0) {
                            continue;
                        }
                        Map<Object, Slot<T>> slots = windowQueues.get(queueIndex).slots;
                        boolean sizeTrigger = slots.size() >= maxAccumulateSize;
                        if (now < flushTime && !sizeTrigger && !backlogTrigger) {
                            nextDeadline = Math.min(nextDeadline, flushTime);
                            continue;
                        }
                        if (!tryLockQueue(batch, queueIndex)) {
                            continue;
                        }
                        drainSlots(batch, slots);
                        //还有残留说明本批没取完，或者遍历期间有新提交，标记为立即到期，下一轮继续处理；
                        //取空则回到空闲状态，等下一个提交重新开窗
                        nextFlushTime.set(slots.isEmpty() ? 0 : now);
                    }
                    if (batch.tasks.isEmpty()) {
                        release(batch.queueIndexes);
                        long waitMillis = idleWaitMillis;
                        if (nextDeadline != Long.MAX_VALUE) {
                            waitMillis = Math.max(1, Math.min(idleWaitMillis, nextDeadline - now));
                        }
                        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(waitMillis));
                        continue;
                    }
                    handleBatch(batch);
                } catch (Exception e) {
                    logger.error("camellia merging executor error, name = {}", name, e);
                } finally {
                    if (batch != null) {
                        release(batch.queueIndexes);
                    }
                }
            }
            flushRemainingOnShutdown();
            logger.warn("camellia merging executor work thread shutdown, thread.name = {}, skip.task.size = {}", getName(), getQueueSize());
        }

        /**
         * 停机时把还停留在窗口内的槽位做一次最终刷新，避免丢失更新；
         * 多个工作线程并发进入也没问题：队列级别的 queueRunning 保证同一个队列只被刷新一次
         */
        private void flushRemainingOnShutdown() {
            Batch<T> batch = new Batch<>(maxBatchSize);
            for (int queueIndex = 0; queueIndex < queueCount; queueIndex++) {
                if (queueNextFlushTime[queueIndex].get() == 0) continue;
                if (!queueRunning[queueIndex].compareAndSet(false, true)) continue;
                try {
                    drainSlots(batch, windowQueues.get(queueIndex).slots);
                    if (!batch.contains(queueIndex)) {
                        batch.addQueueIndex(queueIndex);
                    }
                } finally {
                    queueRunning[queueIndex].set(false);
                }
                if (batch.tasks.size() >= maxBatchSize) {
                    handleBatchQuietly(batch);
                    batch = new Batch<>(maxBatchSize);
                }
            }
            handleBatchQuietly(batch);
        }

        /**
         * 取出槽位：用 remove(key, snapshot) 保证"遍历期间被合并过（对象已被替换）的 key"留到下一轮，不会丢更新
         */
        private void drainSlots(Batch<T> batch, Map<Object, Slot<T>> slots) {
            for (Map.Entry<Object, Slot<T>> entry : slots.entrySet()) {
                if (batch.tasks.size() >= maxBatchSize) {
                    break;
                }
                Slot<T> snapshot = entry.getValue();
                if (slots.remove(entry.getKey(), snapshot)) {
                    batch.tasks.add(snapshot.value);
                }
            }
        }

        private boolean tryLockQueue(Batch<T> batch, int queueIndex) {
            if (batch.contains(queueIndex)) {
                return true;
            }
            boolean success = queueRunning[queueIndex].compareAndSet(false, true);
            if (success) {
                batch.addQueueIndex(queueIndex);
            }
            return success;
        }

        private void handleBatch(Batch<T> batch) throws Exception {
            active.set(true);
            inflightTaskCount.addAndGet(batch.tasks.size());
            handlingQueueIndexes.set(batch.queueIndexes);
            try {
                batchHandler.handle(batch.tasks);
                completedTaskCount.addAndGet(batch.tasks.size());
            } finally {
                handlingQueueIndexes.remove();
                inflightTaskCount.addAndGet(-batch.tasks.size());
                active.set(false);
            }
        }

        private void handleBatchQuietly(Batch<T> batch) {
            if (batch.tasks.isEmpty()) return;
            try {
                handleBatch(batch);
            } catch (Exception e) {
                logger.error("camellia merging executor flush on shutdown error, name = {}", name, e);
            }
        }
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

    private static class WindowQueue<T> {
        private final Map<Object, Slot<T>> slots = new ConcurrentHashMap<>();
    }

    private static class Slot<T> {
        private final T value;

        private Slot(T value) {
            this.value = value;
        }
    }

    /**
     * 相同 hashKey 的合并规则：oldValue 是已累积的值，newValue 是新提交的值
     */
    public static interface Merger<T> {
        T merge(T oldValue, T newValue);
    }

    public static interface BatchHandler<T> {
        void handle(List<T> tasks) throws Exception;
    }

    public static interface RejectedExecutionHandler<T> {
        void rejectedExecution(Object hashKey, T task, CamelliaMergingExecutor<T> executor);
    }

    public static class AbortPolicy<T> implements RejectedExecutionHandler<T> {
        @Override
        public void rejectedExecution(Object hashKey, T task, CamelliaMergingExecutor<T> executor) {
            throw new RejectedExecutionException("Task " + task + " rejected from " + executor.name + ", hashKey = " + hashKey);
        }
    }

    public static class DiscardPolicy<T> implements RejectedExecutionHandler<T> {
        @Override
        public void rejectedExecution(Object hashKey, T task, CamelliaMergingExecutor<T> executor) {
            logger.warn("Task {} is discard from {}, hashKey = {}", task, executor.name, hashKey);
        }
    }

    public static class CallerRunsPolicy<T> implements RejectedExecutionHandler<T> {
        @Override
        public void rejectedExecution(Object hashKey, T task, CamelliaMergingExecutor<T> executor) {
            executor.callerRuns(hashKey, task);
        }
    }
}
