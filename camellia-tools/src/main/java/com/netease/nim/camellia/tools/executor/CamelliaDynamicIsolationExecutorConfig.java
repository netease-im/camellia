package com.netease.nim.camellia.tools.executor;

import com.netease.nim.camellia.tools.base.DynamicConfig;
import com.netease.nim.camellia.tools.base.DynamicValueGetter;

import java.util.Collections;
import java.util.Set;

/**
 * Created by caojiajun on 2023/1/4
 */
public class CamelliaDynamicIsolationExecutorConfig {

    private static final long defaultStatisticSlidingWindowTime = 10*1000L;
    private static final int defaultStatisticSlidingWindowBucketSize = 10;
    private static final double defaultIsolationThresholdPercentage = 0.3;
    private static final long defaultTaskSpendThresholdMs = 1000L;
    private static final int defaultMaxIsolationKeyCount = 4096;
    private static final long defaultTargetLatencyMs = 300L;
    private static final int defaultTaskExpireTimeMs = -1;
    private static final int defaultMaxDepth = 128;
    private static final String PREFIX = "camellia.dynamic.isolation.executor.config";

    private String name;
    private DynamicValueGetter<Integer> poolSize;
    private DynamicValueGetter<Integer> queueSize = () -> Integer.MAX_VALUE;
    private long statisticSlidingWindowTime = defaultStatisticSlidingWindowTime;//统计成功失败的滑动窗口的大小，单位ms，默认10s
    private int statisticSlidingWindowBucketSize = defaultStatisticSlidingWindowBucketSize;//滑动窗口分割为多少个bucket，默认10个
    private DynamicValueGetter<Double> isolationThresholdPercentage = () -> defaultIsolationThresholdPercentage;//任务占用线程池比例达到多少进入隔离线程池
    private DynamicValueGetter<Long> taskSpendThresholdMs = () -> defaultTaskSpendThresholdMs;//任务执行的耗时的阈值（小于算fast，大于算slow）
    private int maxIsolationKeyCount = defaultMaxIsolationKeyCount;//预计的最大IsolationKey的数量
    private DynamicValueGetter<Set<String>> whiteListIsolationKeys = Collections::emptySet;
    private DynamicValueGetter<Long> targetLatencyMs = () -> defaultTargetLatencyMs;//目标的任务执行延迟
    private DynamicValueGetter<Integer> taskExpireTimeMs = () -> defaultTaskExpireTimeMs;//任务如果直到过期都没有能够轮到执行，则任务会被直接丢弃，如果小于0，则表示不过期，默认不过期
    private DynamicValueGetter<Integer> maxDepth = () -> defaultMaxDepth;//切换线程池的最大次数

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
        this.isolationThresholdPercentage = DynamicConfig.wrapper(dynamicConfig, PREFIX + "." + name + ".isolationThresholdPercentage", defaultIsolationThresholdPercentage);
        this.taskSpendThresholdMs = DynamicConfig.wrapper(dynamicConfig, PREFIX + "." + name + ".taskSpendThresholdMs", defaultTaskSpendThresholdMs);
        this.targetLatencyMs = DynamicConfig.wrapper(dynamicConfig, PREFIX + "." + name + ".targetLatencyMs", defaultTargetLatencyMs);
        this.taskExpireTimeMs = DynamicConfig.wrapper(dynamicConfig, PREFIX + "." + name + ".taskExpireTimeMs", defaultTaskExpireTimeMs);
        this.maxDepth = DynamicConfig.wrapper(dynamicConfig, PREFIX + "." + name + ".maxDepth", defaultMaxDepth);
    }

    public CamelliaDynamicIsolationExecutorConfig(String name, DynamicConfig dynamicConfig, int poolSize, int queueSize) {
        this.name = name;
        this.poolSize = DynamicConfig.wrapper(dynamicConfig, PREFIX + "." + name + ".poolSize", poolSize);
        this.queueSize = DynamicConfig.wrapper(dynamicConfig, PREFIX + "." + name + ".queueSize", queueSize);
        this.isolationThresholdPercentage = DynamicConfig.wrapper(dynamicConfig, PREFIX + "." + name + ".isolationThresholdPercentage", defaultIsolationThresholdPercentage);
        this.taskSpendThresholdMs = DynamicConfig.wrapper(dynamicConfig, PREFIX + "." + name + ".taskSpendThresholdMs", defaultTaskSpendThresholdMs);
        this.targetLatencyMs = DynamicConfig.wrapper(dynamicConfig, PREFIX + "." + name + ".targetLatencyMs", defaultTargetLatencyMs);
        this.taskExpireTimeMs = DynamicConfig.wrapper(dynamicConfig, PREFIX + "." + name + ".taskExpireTimeMs", defaultTaskExpireTimeMs);
        this.maxDepth = DynamicConfig.wrapper(dynamicConfig, PREFIX + "." + name + ".maxDepth", defaultMaxDepth);
    }

    public CamelliaDynamicIsolationExecutorConfig(String name, DynamicConfig dynamicConfig, int poolSize, int queueSize, double isolationThresholdPercentage,
                                                  long taskSpendThresholdMs, long targetLatencyMs, int taskExpireTimeMs, int maxDepth) {
        this.name = name;
        this.poolSize = DynamicConfig.wrapper(dynamicConfig, PREFIX + "." + name + ".poolSize", poolSize);
        this.queueSize = DynamicConfig.wrapper(dynamicConfig, PREFIX + "." + name + ".queueSize", queueSize);
        this.isolationThresholdPercentage = DynamicConfig.wrapper(dynamicConfig, PREFIX + "." + name + ".isolationThresholdPercentage", isolationThresholdPercentage);
        this.taskSpendThresholdMs = DynamicConfig.wrapper(dynamicConfig, PREFIX + "." + name + ".taskSpendThresholdMs", taskSpendThresholdMs);
        this.targetLatencyMs = DynamicConfig.wrapper(dynamicConfig, PREFIX + "." + name + ".targetLatencyMs", targetLatencyMs);
        this.taskExpireTimeMs = DynamicConfig.wrapper(dynamicConfig, PREFIX + "." + name + ".taskExpireTimeMs", taskExpireTimeMs);
        this.maxDepth = DynamicConfig.wrapper(dynamicConfig, PREFIX + "." + name + ".maxDepth", maxDepth);
    }

    public CamelliaDynamicIsolationExecutorConfig(String name, DynamicValueGetter<Integer> poolSize, DynamicValueGetter<Integer> queueSize,
                                                  long statisticSlidingWindowTime, int statisticSlidingWindowBucketSize, DynamicValueGetter<Double> isolationThresholdPercentage,
                                                  DynamicValueGetter<Long> taskSpendThresholdMs, int maxIsolationKeyCount, DynamicValueGetter<Set<String>> whiteListIsolationKeys,
                                                  DynamicValueGetter<Long> targetLatencyMs, DynamicValueGetter<Integer> taskExpireTimeMs, DynamicValueGetter<Integer> maxDepth) {
        this.name = name;
        this.poolSize = poolSize;
        this.queueSize = queueSize;
        this.statisticSlidingWindowTime = statisticSlidingWindowTime;
        this.statisticSlidingWindowBucketSize = statisticSlidingWindowBucketSize;
        this.isolationThresholdPercentage = isolationThresholdPercentage;
        this.taskSpendThresholdMs = taskSpendThresholdMs;
        this.maxIsolationKeyCount = maxIsolationKeyCount;
        this.whiteListIsolationKeys = whiteListIsolationKeys;
        this.targetLatencyMs = targetLatencyMs;
        this.taskExpireTimeMs = taskExpireTimeMs;
        this.maxDepth = maxDepth;
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

    public DynamicValueGetter<Long> getTargetLatencyMs() {
        return targetLatencyMs;
    }

    public void setTargetLatencyMs(DynamicValueGetter<Long> targetLatencyMs) {
        this.targetLatencyMs = targetLatencyMs;
    }

    public DynamicValueGetter<Integer> getTaskExpireTimeMs() {
        return taskExpireTimeMs;
    }

    public void setTaskExpireTimeMs(DynamicValueGetter<Integer> taskExpireTimeMs) {
        this.taskExpireTimeMs = taskExpireTimeMs;
    }

    public DynamicValueGetter<Integer> getMaxDepth() {
        return maxDepth;
    }

    public void setMaxDepth(DynamicValueGetter<Integer> maxDepth) {
        this.maxDepth = maxDepth;
    }
}
