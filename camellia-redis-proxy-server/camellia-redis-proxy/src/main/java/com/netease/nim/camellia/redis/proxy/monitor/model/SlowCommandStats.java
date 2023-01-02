package com.netease.nim.camellia.redis.proxy.monitor.model;

/**
 * Created by caojiajun on 2022/9/16
 */
public class SlowCommandStats {
    private String bid;
    private String bgroup;
    private String command;
    private String keys;
    private double spendMillis;
    private long thresholdMillis;

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

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getKeys() {
        return keys;
    }

    public void setKeys(String keys) {
        this.keys = keys;
    }

    public double getSpendMillis() {
        return spendMillis;
    }

    public void setSpendMillis(double spendMillis) {
        this.spendMillis = spendMillis;
    }

    public long getThresholdMillis() {
        return thresholdMillis;
    }

    public void setThresholdMillis(long thresholdMillis) {
        this.thresholdMillis = thresholdMillis;
    }
}
