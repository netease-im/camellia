package com.netease.nim.camellia.redis.proxy.monitor.model;

/**
 * Created by caojiajun on 2022/9/16
 */
public class UpstreamRedisSpendStats {
    private String addr;
    private long count;
    private double avgSpendMs;
    private double maxSpendMs;
    private double spendMsP50;
    private double spendMsP75;
    private double spendMsP90;
    private double spendMsP95;
    private double spendMsP99;
    private double spendMsP999;

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

    public double getSpendMsP50() {
        return spendMsP50;
    }

    public void setSpendMsP50(double spendMsP50) {
        this.spendMsP50 = spendMsP50;
    }

    public double getSpendMsP75() {
        return spendMsP75;
    }

    public void setSpendMsP75(double spendMsP75) {
        this.spendMsP75 = spendMsP75;
    }

    public double getSpendMsP90() {
        return spendMsP90;
    }

    public void setSpendMsP90(double spendMsP90) {
        this.spendMsP90 = spendMsP90;
    }

    public double getSpendMsP95() {
        return spendMsP95;
    }

    public void setSpendMsP95(double spendMsP95) {
        this.spendMsP95 = spendMsP95;
    }

    public double getSpendMsP99() {
        return spendMsP99;
    }

    public void setSpendMsP99(double spendMsP99) {
        this.spendMsP99 = spendMsP99;
    }

    public double getSpendMsP999() {
        return spendMsP999;
    }

    public void setSpendMsP999(double spendMsP999) {
        this.spendMsP999 = spendMsP999;
    }
}
