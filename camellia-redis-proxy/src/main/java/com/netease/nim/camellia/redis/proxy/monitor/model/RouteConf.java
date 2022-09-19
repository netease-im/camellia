package com.netease.nim.camellia.redis.proxy.monitor.model;

/**
 * Created by caojiajun on 2022/9/16
 */
public class RouteConf {
    private Long bid;
    private String bgroup;
    private String resourceTable;
    private long updateTime;

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

    public String getResourceTable() {
        return resourceTable;
    }

    public void setResourceTable(String resourceTable) {
        this.resourceTable = resourceTable;
    }

    public long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }
}
