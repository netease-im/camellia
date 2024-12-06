package com.netease.nim.camellia.redis.proxy.monitor.model;

/**
 * Created by caojiajun on 2023/1/9
 */
public class BidBgroupConnectStats {
    private Long bid;
    private String bgroup;
    private long connect;

    /**
     * default constructor
     */
    public BidBgroupConnectStats() {
    }

    /**
     * get bid
     * @return bid
     */
    public Long getBid() {
        return bid;
    }

    /**
     * set bid
     * @param bid bid
     */
    public void setBid(Long bid) {
        this.bid = bid;
    }

    /**
     * get bgroup
     * @return bgroup
     */
    public String getBgroup() {
        return bgroup;
    }

    /**
     * set bgroup
     * @param bgroup bgroup
     */
    public void setBgroup(String bgroup) {
        this.bgroup = bgroup;
    }

    /**
     * get connect
     * @return connect
     */
    public long getConnect() {
        return connect;
    }

    /**
     * set connect
     * @param connect connect
     */
    public void setConnect(long connect) {
        this.connect = connect;
    }
}
