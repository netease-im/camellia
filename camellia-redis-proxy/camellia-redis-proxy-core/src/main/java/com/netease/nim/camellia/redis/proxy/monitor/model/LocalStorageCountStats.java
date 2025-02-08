package com.netease.nim.camellia.redis.proxy.monitor.model;

/**
 * Created by caojiajun on 2025/2/8
 */
public class LocalStorageCountStats {

    private String item;
    private long count;

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}
