package com.netease.nim.camellia.redis.proxy.monitor.model;


/**
 * Created by caojiajun on 2022/9/16
 */
public class HotKeyStats {
    private String bid;
    private String bgroup;
    private String key;
    private long count;
    private long times;
    private long max;
    private double avg;
    private long checkMillis;
    private long checkThreshold;


    public String getBid() {
        return bid;
    }

    public void setBid(String bid) {
        this.bid = bid;
    }

    public String getBgroup() {
        return bgroup;
    }

    public void setBgroup(String bgroup) {
        this.bgroup = bgroup;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public long getTimes() {
        return times;
    }

    public void setTimes(long times) {
        this.times = times;
    }

    public long getMax() {
        return max;
    }

    public void setMax(long max) {
        this.max = max;
    }

    public double getAvg() {
        return avg;
    }

    public void setAvg(double avg) {
        this.avg = avg;
    }

    public long getCheckMillis() {
        return checkMillis;
    }

    public void setCheckMillis(long checkMillis) {
        this.checkMillis = checkMillis;
    }

    public long getCheckThreshold() {
        return checkThreshold;
    }

    public void setCheckThreshold(long checkThreshold) {
        this.checkThreshold = checkThreshold;
    }
}
