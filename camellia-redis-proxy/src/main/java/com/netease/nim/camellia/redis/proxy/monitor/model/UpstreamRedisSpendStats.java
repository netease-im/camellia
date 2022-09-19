package com.netease.nim.camellia.redis.proxy.monitor.model;

/**
 * Created by caojiajun on 2022/9/16
 */
public class UpstreamRedisSpendStats {
    private String addr;
    private long count;
    private double avgSpendMs;
    private double maxSpendMs;

    public String getAddr() {
        return addr;
    }

    public void setAddr(String addr) {
        this.addr = addr;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public double getAvgSpendMs() {
        return avgSpendMs;
    }

    public void setAvgSpendMs(double avgSpendMs) {
        this.avgSpendMs = avgSpendMs;
    }

    public double getMaxSpendMs() {
        return maxSpendMs;
    }

    public void setMaxSpendMs(double maxSpendMs) {
        this.maxSpendMs = maxSpendMs;
    }
}
