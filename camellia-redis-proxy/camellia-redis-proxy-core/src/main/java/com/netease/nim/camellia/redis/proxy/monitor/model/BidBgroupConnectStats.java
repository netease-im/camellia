package com.netease.nim.camellia.redis.proxy.monitor.model;

/**
 * Created by caojiajun on 2023/1/9
 */
public class BidBgroupConnectStats {
    private Long bid;
    private String bgroup;
    private long connect;

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

    public long getConnect() {
        return connect;
    }

    public void setConnect(long connect) {
        this.connect = connect;
    }
}
