package com.netease.nim.camellia.redis.proxy.monitor.model;

/**
 * Created by caojiajun on 2025/2/8
 */
public class LocalStorageTimeStats {

    private String item;
    private TimeStats timeStats;

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public TimeStats getTimeStats() {
        return timeStats;
    }

    public void setTimeStats(TimeStats timeStats) {
        this.timeStats = timeStats;
    }
}
