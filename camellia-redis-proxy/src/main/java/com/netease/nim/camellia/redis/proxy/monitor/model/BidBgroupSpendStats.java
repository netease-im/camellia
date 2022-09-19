package com.netease.nim.camellia.redis.proxy.monitor.model;

/**
 * Created by caojiajun on 2022/9/16
 */
public class BidBgroupSpendStats {
    private Long bid;
    private String bgroup;
    private String command;
    private long count;
    private double avgSpendMs;
    private double maxSpendMs;

    public Long getBid() {
        return bid;
    }

    public void setBid(Long bid) {
        this.bid = bid;
    }

    public String getBgroup() {
        return bgroup;
    }

    public void setBgroup(String bgroup) {
        this.bgroup = bgroup;
    }

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
