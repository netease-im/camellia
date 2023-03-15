package com.netease.nim.camellia.redis.proxy.monitor.model;

/**
 * Created by caojiajun on 2023/3/13
 */
public class UpstreamFailStats {
    private String resource;
    private String command;
    private String msg;
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

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}
