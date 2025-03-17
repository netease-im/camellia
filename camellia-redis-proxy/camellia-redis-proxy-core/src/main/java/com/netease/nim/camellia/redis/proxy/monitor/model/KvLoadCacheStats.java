package com.netease.nim.camellia.redis.proxy.monitor.model;


/**
 * Created by caojiajun on 2025/3/14
 */
public class KvLoadCacheStats {
    private String namespace;
    private String command;
    private long count;

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
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
}
