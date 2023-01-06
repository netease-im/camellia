package com.netease.nim.camellia.tools.executor;

import com.netease.nim.camellia.tools.base.DynamicConfig;
import com.netease.nim.camellia.tools.base.DynamicValueGetter;

import java.util.Collections;
import java.util.Set;

/**
 * Created by caojiajun on 2023/1/4
 */
public class CamelliaDynamicIsolationExecutorConfig {

    private static final String PREFIX = "camellia.dynamic.isolation.executor.config";

    private String name;
    private DynamicValueGetter<Integer> poolSize;
    private DynamicValueGetter<Integer> queueSize = () -> Integer.MAX_VALUE;
    private long statisticSlidingWindowTime = 10*1000L;//统计成功失败的滑动窗口的大小，单位ms，默认10s
    private int statisticSlidingWindowBucketSize = 10;//滑动窗口分割为多少个bucket，默认10个
    private DynamicValueGetter<Double> isolationThresholdPercentage = () -> 0.5;//任务占用线程池比例达到多少进入隔离线程池
    private DynamicValueGetter<Long> taskSpendThresholdMs = () -> 1000L;//任务执行的耗时的阈值（小于算fast，大于算slow）
    private int maxIsolationKeyCount = 4096;//预计的最大IsolationKey的数量
    private DynamicValueGetter<Set<String>> whiteListIsolationKeys = Collections::emptySet;

    public CamelliaDynamicIsolationExecutorConfig(String name, DynamicValueGetter<Integer> poolSize) {
        this.name = name;
        this.poolSize = poolSize;
    }

    public CamelliaDynamicIsolationExecutorConfig(String name, DynamicValueGetter<Integer> poolSize, DynamicValueGetter<Integer> queueSize) {
        this.name = name;
        this.poolSize = poolSize;
        this.queueSize = queueSize;
    }

    public CamelliaDynamicIsolationExecutorConfig(String name, DynamicConfig dynamicConfig, int poolSize) {
        this.name = name;
        this.poolSize = DynamicConfig.wrapper(dynamicConfig, PREFIX + "." + name + ".poolSize", poolSize);
        this.queueSize = DynamicConfig.wrapper(dynamicConfig, PREFIX + "." + name + ".queueSize", Integer.MAX_VALUE);
        this.isolationThresholdPercentage = DynamicConfig.wrapper(dynamicConfig, PREFIX + "." + name + ".isolationThresholdPercentage", 0.5);
        this.taskSpendThresholdMs = DynamicConfig.wrapper(dynamicConfig, PREFIX + "." + name + ".taskSpendThresholdMs", 1000L);
    }

    public CamelliaDynamicIsolationExecutorConfig(String name, DynamicConfig dynamicConfig, int poolSize, int queueSize) {
        this.name = name;
        this.poolSize = DynamicConfig.wrapper(dynamicConfig, PREFIX + "." + name + ".poolSize", poolSize);
        this.queueSize = DynamicConfig.wrapper(dynamicConfig, PREFIX + "." + name + ".queueSize", queueSize);
        this.isolationThresholdPercentage = DynamicConfig.wrapper(dynamicConfig, PREFIX + "." + name + ".isolationThresholdPercentage", 0.5);
        this.taskSpendThresholdMs = DynamicConfig.wrapper(dynamicConfig, PREFIX + "." + name + ".taskSpendThresholdMs", 1000L);
    }

    public CamelliaDynamicIsolationExecutorConfig(String name, DynamicConfig dynamicConfig, int poolSize, int queueSize, double isolationThresholdPercentage, long taskSpendThresholdMs) {
        this.name = name;
        this.poolSize = DynamicConfig.wrapper(dynamicConfig, PREFIX + "." + name + ".poolSize", poolSize);
        this.queueSize = DynamicConfig.wrapper(dynamicConfig, PREFIX + "." + name + ".queueSize", queueSize);
        this.isolationThresholdPercentage = DynamicConfig.wrapper(dynamicConfig, PREFIX + "." + name + ".isolationThresholdPercentage", isolationThresholdPercentage);
        this.taskSpendThresholdMs = DynamicConfig.wrapper(dynamicConfig, PREFIX + "." + name + ".taskSpendThresholdMs", taskSpendThresholdMs);
    }

    public CamelliaDynamicIsolationExecutorConfig(String name, DynamicValueGetter<Integer> poolSize, DynamicValueGetter<Integer> queueSize,
                                                  long statisticSlidingWindowTime, int statisticSlidingWindowBucketSize,
                                                  DynamicValueGetter<Double> isolationThresholdPercentage, DynamicValueGetter<Long> taskSpendThresholdMs,
                                                  int maxIsolationKeyCount, DynamicValueGetter<Set<String>> whiteListIsolationKeys) {
        this.name = name;
        this.poolSize = poolSize;
        this.queueSize = queueSize;
        this.statisticSlidingWindowTime = statisticSlidingWindowTime;
        this.statisticSlidingWindowBucketSize = statisticSlidingWindowBucketSize;
        this.isolationThresholdPercentage = isolationThresholdPercentage;
        this.taskSpendThresholdMs = taskSpendThresholdMs;
        this.maxIsolationKeyCount = maxIsolationKeyCount;
        this.whiteListIsolationKeys = whiteListIsolationKeys;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DynamicValueGetter<Integer> getPoolSize() {
        return poolSize;
    }

    public void setPoolSize(DynamicValueGetter<Integer> poolSize) {
        this.poolSize = poolSize;
    }

    public DynamicValueGetter<Integer> getQueueSize() {
        return queueSize;
    }

    public void setQueueSize(DynamicValueGetter<Integer> queueSize) {
        this.queueSize = queueSize;
    }

    public long getStatisticSlidingWindowTime() {
        return statisticSlidingWindowTime;
    }

    public void setStatisticSlidingWindowTime(long statisticSlidingWindowTime) {
        this.statisticSlidingWindowTime = statisticSlidingWindowTime;
    }

    public int getStatisticSlidingWindowBucketSize() {
        return statisticSlidingWindowBucketSize;
    }

    public void setStatisticSlidingWindowBucketSize(int statisticSlidingWindowBucketSize) {
        this.statisticSlidingWindowBucketSize = statisticSlidingWindowBucketSize;
    }

    public DynamicValueGetter<Double> getIsolationThresholdPercentage() {
        return isolationThresholdPercentage;
    }

    public void setIsolationThresholdPercentage(DynamicValueGetter<Double> isolationThresholdPercentage) {
        this.isolationThresholdPercentage = isolationThresholdPercentage;
    }

    public DynamicValueGetter<Long> getTaskSpendThresholdMs() {
        return taskSpendThresholdMs;
    }

    public void setTaskSpendThresholdMs(DynamicValueGetter<Long> taskSpendThresholdMs) {
        this.taskSpendThresholdMs = taskSpendThresholdMs;
    }

    public int getMaxIsolationKeyCount() {
        return maxIsolationKeyCount;
    }

    public void setMaxIsolationKeyCount(int maxIsolationKeyCount) {
        this.maxIsolationKeyCount = maxIsolationKeyCount;
    }

    public DynamicValueGetter<Set<String>> getWhiteListIsolationKeys() {
        return whiteListIsolationKeys;
    }

    public void setWhiteListIsolationKeys(DynamicValueGetter<Set<String>> whiteListIsolationKeys) {
        this.whiteListIsolationKeys = whiteListIsolationKeys;
    }
}
