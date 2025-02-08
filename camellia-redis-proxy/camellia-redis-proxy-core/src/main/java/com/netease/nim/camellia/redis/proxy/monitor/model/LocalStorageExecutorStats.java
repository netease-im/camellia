package com.netease.nim.camellia.redis.proxy.monitor.model;


public class LocalStorageExecutorStats {

    private String name;
    private int pending;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPending() {
        return pending;
    }

    public void setPending(int pending) {
        this.pending = pending;
    }
}
