package com.netease.nim.camellia.tools.circuitbreaker;

import com.netease.nim.camellia.core.util.DynamicValueGetter;

/**
 * Created by caojiajun on 2022/3/25
 */
public class CircuitBreakerConfig {

    //以下参数一经设置不可修改
    private String name = "camellia-circuit-breaker";//熔断器的别名
    private long statisticSlidingWindowTime = 10*1000L;//统计成功失败的滑动窗口的大小，单位ms，默认10s
    private int statisticSlidingWindowBucketSize = 10;//滑动窗口分割为多少个bucket，默认10个

    //以下参数可以动态修改
    private DynamicValueGetter<Boolean> enable = () -> true;//是否启用，若不启用，则不进行失败率统计，所有请求都允许
    private DynamicValueGetter<Boolean> forceOpen = () -> false;//强制打开，则所有请求都不允许
    private DynamicValueGetter<Double> failThresholdPercentage = () -> 0.5;//滑动窗口范围内失败比例超过多少触发熔断，默认50%
    private DynamicValueGetter<Long> requestVolumeThreshold = () -> 20L;//滑动窗口内至少多少个请求才会触发熔断，默认20个
    private DynamicValueGetter<Long> singleTestIntervalMillis = () -> 5000L;//当熔断器打开的情况下，间隔多久尝试一次探测（也就是半开）
    private DynamicValueGetter<Boolean> logEnable = () -> true;//是否打开日志（主要是打印熔断器状态变更时打印）

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public DynamicValueGetter<Boolean> getEnable() {
        return enable;
    }

    public void setEnable(DynamicValueGetter<Boolean> enable) {
        this.enable = enable;
    }

    public DynamicValueGetter<Boolean> getForceOpen() {
        return forceOpen;
    }

    public void setForceOpen(DynamicValueGetter<Boolean> forceOpen) {
        this.forceOpen = forceOpen;
    }

    public DynamicValueGetter<Double> getFailThresholdPercentage() {
        return failThresholdPercentage;
    }

    public void setFailThresholdPercentage(DynamicValueGetter<Double> failThresholdPercentage) {
        this.failThresholdPercentage = failThresholdPercentage;
    }

    public DynamicValueGetter<Long> getRequestVolumeThreshold() {
        return requestVolumeThreshold;
    }

    public void setRequestVolumeThreshold(DynamicValueGetter<Long> requestVolumeThreshold) {
        this.requestVolumeThreshold = requestVolumeThreshold;
    }

    public DynamicValueGetter<Long> getSingleTestIntervalMillis() {
        return singleTestIntervalMillis;
    }

    public void setSingleTestIntervalMillis(DynamicValueGetter<Long> singleTestIntervalMillis) {
        this.singleTestIntervalMillis = singleTestIntervalMillis;
    }

    public DynamicValueGetter<Boolean> getLogEnable() {
        return logEnable;
    }

    public void setLogEnable(DynamicValueGetter<Boolean> logEnable) {
        this.logEnable = logEnable;
    }
}
