package com.netease.nim.camellia.tools.executor;

import com.netease.nim.camellia.tools.base.DynamicValueGetter;

/**
 * Created by caojiajun on 2026/10/9
 *
 * CamelliaMergingExecutor 的配置
 */
public class CamelliaMergingExecutorConfig<T> {

    private String name;
    private int queueCount = CamelliaMergingExecutor.defaultQueueCount;
    private int workerCount = CamelliaMergingExecutor.defaultWorkerCount;
    private int maxBatchSize = CamelliaMergingExecutor.defaultMaxBatchSize;
    private int skipWaitPendingThreshold = CamelliaMergingExecutor.defaultSkipWaitPendingThreshold;
    private long idleWaitMillis = CamelliaMergingExecutor.defaultIdleWaitMillis;
    private long windowMillis = CamelliaMergingExecutor.defaultWindowMillis;
    private int maxAccumulateSize = CamelliaMergingExecutor.defaultMaxAccumulateSize;
    private Runnable workThreadInitCallback;
    private CamelliaMergingExecutor.Merger<T> merger;
    private CamelliaMergingExecutor.BatchHandler<T> batchHandler;
    private DynamicValueGetter<CamelliaMergingExecutor.RejectedExecutionHandler<T>> rejectedExecutionHandler = () -> null;

    public CamelliaMergingExecutorConfig(String name, CamelliaMergingExecutor.Merger<T> merger,
                                         CamelliaMergingExecutor.BatchHandler<T> batchHandler) {
        this.name = name;
        this.merger = merger;
        this.batchHandler = batchHandler;
    }

    public CamelliaMergingExecutorConfig(String name, int queueCount, int workerCount, int maxBatchSize,
                                         long windowMillis, CamelliaMergingExecutor.Merger<T> merger,
                                         CamelliaMergingExecutor.BatchHandler<T> batchHandler) {
        this.name = name;
        this.queueCount = queueCount;
        this.workerCount = workerCount;
        this.maxBatchSize = maxBatchSize;
        this.windowMillis = windowMillis;
        this.merger = merger;
        this.batchHandler = batchHandler;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getQueueCount() {
        return queueCount;
    }

    public void setQueueCount(int queueCount) {
        this.queueCount = queueCount;
    }

    public int getWorkerCount() {
        return workerCount;
    }

    public void setWorkerCount(int workerCount) {
        this.workerCount = workerCount;
    }

    public int getMaxBatchSize() {
        return maxBatchSize;
    }

    /**
     * 单次交给 BatchHandler 的合并后条数上限
     */
    public void setMaxBatchSize(int maxBatchSize) {
        this.maxBatchSize = maxBatchSize;
    }

    public int getSkipWaitPendingThreshold() {
        return skipWaitPendingThreshold;
    }

    /**
     * 待合并的不同 key 总数达到该值时，所有队列立即刷新，避免延迟累积；<=0 表示不启用
     */
    public void setSkipWaitPendingThreshold(int skipWaitPendingThreshold) {
        this.skipWaitPendingThreshold = skipWaitPendingThreshold;
    }

    public long getIdleWaitMillis() {
        return idleWaitMillis;
    }

    /**
     * 没有到期队列时工作线程的最大休眠时间，用于控制空转唤醒频率
     */
    public void setIdleWaitMillis(long idleWaitMillis) {
        this.idleWaitMillis = idleWaitMillis;
    }

    public long getWindowMillis() {
        return windowMillis;
    }

    /**
     * 单队列滚动窗口长度：窗口越长合并比越高、写入延迟越大
     */
    public void setWindowMillis(long windowMillis) {
        this.windowMillis = windowMillis;
    }

    public int getMaxAccumulateSize() {
        return maxAccumulateSize;
    }

    /**
     * 提前刷新阈值 + 单队列槽位硬上限，内存上界 = queueCount * maxAccumulateSize；
     * 取值建议不小于"单队列单窗口的不同 key 数"，否则会频繁提前刷新、窗口被压缩
     */
    public void setMaxAccumulateSize(int maxAccumulateSize) {
        this.maxAccumulateSize = maxAccumulateSize;
    }

    public Runnable getWorkThreadInitCallback() {
        return workThreadInitCallback;
    }

    public void setWorkThreadInitCallback(Runnable workThreadInitCallback) {
        this.workThreadInitCallback = workThreadInitCallback;
    }

    public CamelliaMergingExecutor.Merger<T> getMerger() {
        return merger;
    }

    public void setMerger(CamelliaMergingExecutor.Merger<T> merger) {
        this.merger = merger;
    }

    public CamelliaMergingExecutor.BatchHandler<T> getBatchHandler() {
        return batchHandler;
    }

    public void setBatchHandler(CamelliaMergingExecutor.BatchHandler<T> batchHandler) {
        this.batchHandler = batchHandler;
    }

    public DynamicValueGetter<CamelliaMergingExecutor.RejectedExecutionHandler<T>> getRejectedExecutionHandler() {
        return rejectedExecutionHandler;
    }

    /**
     * 拒绝策略：只在"新增 hashKey 且单队列槽位已达 maxAccumulateSize"时触发；
     * 默认 CallerRunsPolicy，即在调用方线程同步执行，不会静默丢数据
     */
    public void setRejectedExecutionHandler(DynamicValueGetter<CamelliaMergingExecutor.RejectedExecutionHandler<T>> rejectedExecutionHandler) {
        this.rejectedExecutionHandler = rejectedExecutionHandler;
    }
}
