package com.netease.nim.camellia.tools.executor;

import com.netease.nim.camellia.tools.base.DynamicConfig;
import com.netease.nim.camellia.tools.base.DynamicValueGetter;

/**
 * Created by caojiajun on 2026/8/10
 */
public class CamelliaHashedBatchExecutorConfig<T> {

    private static final String PREFIX = "camellia.hashed.batch.executor.config";

    private String name;
    private int queueCount = CamelliaHashedBatchExecutor.defaultQueueCount;
    private int workerCount = CamelliaHashedBatchExecutor.defaultWorkerCount;
    private DynamicValueGetter<Integer> dynamicQueueCapacity = () -> CamelliaHashedBatchExecutor.defaultQueueCapacity;
    private int maxBatchSize = CamelliaHashedBatchExecutor.defaultMaxBatchSize;
    private int minBatchSize = CamelliaHashedBatchExecutor.defaultMinBatchSize;
    private long maxBatchWaitMillis = CamelliaHashedBatchExecutor.defaultMaxBatchWaitMillis;
    private int perQueueMaxDrainSize = CamelliaHashedBatchExecutor.defaultPerQueueMaxDrainSize;
    private int skipWaitPendingThreshold = CamelliaHashedBatchExecutor.defaultSkipWaitPendingThreshold;
    private long idleWaitMillis = CamelliaHashedBatchExecutor.defaultIdleWaitMillis;
    private Runnable workThreadInitCallback;
    private CamelliaHashedBatchExecutor.BatchHandler<T> batchHandler;
    private DynamicValueGetter<CamelliaHashedBatchExecutor.RejectedExecutionHandler<T>> rejectedExecutionHandler = () -> null;

    public CamelliaHashedBatchExecutorConfig(String name, CamelliaHashedBatchExecutor.BatchHandler<T> batchHandler) {
        this.name = name;
        this.batchHandler = batchHandler;
    }

    public CamelliaHashedBatchExecutorConfig(String name, int queueCount, int workerCount,
                                             int maxBatchSize, CamelliaHashedBatchExecutor.BatchHandler<T> batchHandler) {
        this.name = name;
        this.queueCount = queueCount;
        this.workerCount = workerCount;
        this.maxBatchSize = maxBatchSize;
        this.batchHandler = batchHandler;
    }

    public CamelliaHashedBatchExecutorConfig(String name, int queueCount, int workerCount,
                                             DynamicConfig config, int queueCapacity, int maxBatchSize,
                                             CamelliaHashedBatchExecutor.BatchHandler<T> batchHandler) {
        this.name = name;
        this.queueCount = queueCount;
        this.workerCount = workerCount;
        this.dynamicQueueCapacity = DynamicConfig.wrapper(config, PREFIX + "." + name + ".queueCapacity", queueCapacity);
        this.maxBatchSize = maxBatchSize;
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

    public DynamicValueGetter<Integer> getDynamicQueueCapacity() {
        return dynamicQueueCapacity;
    }

    public void setDynamicQueueCapacity(DynamicValueGetter<Integer> dynamicQueueCapacity) {
        this.dynamicQueueCapacity = dynamicQueueCapacity;
    }

    public int getMaxBatchSize() {
        return maxBatchSize;
    }

    public void setMaxBatchSize(int maxBatchSize) {
        this.maxBatchSize = maxBatchSize;
    }

    public int getMinBatchSize() {
        return minBatchSize;
    }

    public void setMinBatchSize(int minBatchSize) {
        this.minBatchSize = minBatchSize;
    }

    public long getMaxBatchWaitMillis() {
        return maxBatchWaitMillis;
    }

    public void setMaxBatchWaitMillis(long maxBatchWaitMillis) {
        this.maxBatchWaitMillis = maxBatchWaitMillis;
    }

    public int getPerQueueMaxDrainSize() {
        return perQueueMaxDrainSize;
    }

    public void setPerQueueMaxDrainSize(int perQueueMaxDrainSize) {
        this.perQueueMaxDrainSize = perQueueMaxDrainSize;
    }

    public int getSkipWaitPendingThreshold() {
        return skipWaitPendingThreshold;
    }

    public void setSkipWaitPendingThreshold(int skipWaitPendingThreshold) {
        this.skipWaitPendingThreshold = skipWaitPendingThreshold;
    }

    public long getIdleWaitMillis() {
        return idleWaitMillis;
    }

    public void setIdleWaitMillis(long idleWaitMillis) {
        this.idleWaitMillis = idleWaitMillis;
    }

    public Runnable getWorkThreadInitCallback() {
        return workThreadInitCallback;
    }

    public void setWorkThreadInitCallback(Runnable workThreadInitCallback) {
        this.workThreadInitCallback = workThreadInitCallback;
    }

    public CamelliaHashedBatchExecutor.BatchHandler<T> getBatchHandler() {
        return batchHandler;
    }

    public void setBatchHandler(CamelliaHashedBatchExecutor.BatchHandler<T> batchHandler) {
        this.batchHandler = batchHandler;
    }

    public DynamicValueGetter<CamelliaHashedBatchExecutor.RejectedExecutionHandler<T>> getRejectedExecutionHandler() {
        return rejectedExecutionHandler;
    }

    public void setRejectedExecutionHandler(DynamicValueGetter<CamelliaHashedBatchExecutor.RejectedExecutionHandler<T>> rejectedExecutionHandler) {
        this.rejectedExecutionHandler = rejectedExecutionHandler;
    }
}
