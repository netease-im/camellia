package com.netease.nim.camellia.redis.proxy.monitor.model;

/**
 * Created by caojiajun on 2022/9/16
 */
public class SpendStats {
    private String command;
    private long count;
    private double avgSpendMs;
    private double maxSpendMs;

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
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
