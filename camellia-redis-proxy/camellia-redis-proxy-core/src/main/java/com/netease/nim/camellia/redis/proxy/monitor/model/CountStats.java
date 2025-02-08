package com.netease.nim.camellia.redis.proxy.monitor.model;

/**
 * Created by caojiajun on 2025/2/8
 */
public class CountStats {

    private long count;
    private double avg;
    private long max;

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public double getAvg() {
        return avg;
    }

    public void setAvg(double avg) {
        this.avg = avg;
    }

    public long getMax() {
        return max;
    }

    public void setMax(long max) {
        this.max = max;
    }
}
