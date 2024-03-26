package com.netease.nim.camellia.mq.isolation.core.config;

/**
 * Created by caojiajun on 2024/3/26
 */
public class TimeRange {
    private long min = 0;
    private long max = Long.MAX_VALUE;

    public long getMin() {
        return min;
    }

    public void setMin(long min) {
        this.min = min;
    }

    public long getMax() {
        return max;
    }

    public void setMax(long max) {
        this.max = max;
    }
}
