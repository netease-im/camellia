package com.netease.nim.camellia.redis.proxy.monitor.model;

/**
 * Created by caojiajun on 2022/9/16
 */
public class BidBgroupStats {
    private Long bid;
    private String bgroup;
    private long count;

    /**
     * default constructor
     */
    public BidBgroupStats() {
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
     * set brgoup
     * @param bgroup bgroup
     */
    public void setBgroup(String bgroup) {
        this.bgroup = bgroup;
    }

    /**
     * get count
     * @return count
     */
    public long getCount() {
        return count;
    }

    /**
     * set count
     * @param count count
     */
    public void setCount(long count) {
        this.count = count;
    }
}
