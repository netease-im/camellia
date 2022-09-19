package com.netease.nim.camellia.redis.proxy.monitor.model;

/**
 * Created by caojiajun on 2022/9/16
 */
public class ResourceCommandStats {
    private String resource;
    private String command;
    private long count;

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
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
