package com.netease.nim.camellia.redis.proxy.monitor.model;


/**
 * Created by caojiajun on 2022/9/16
 */
public class HotKeyCacheStats {
    private String bid;
    private String bgroup;
    private String key;
    private long hitCount;
    private long checkMillis;
    private long checkThreshold;

    public String getBid() {
        return bid;
    }

    public void setBid(String bid) {
        this.bid = bid;
    }

    public String getBgroup() {
        return bgroup;
    }

    public void setBgroup(String bgroup) {
        this.bgroup = bgroup;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public long getHitCount() {
        return hitCount;
    }

    public void setHitCount(long hitCount) {
        this.hitCount = hitCount;
    }

    public long getCheckMillis() {
        return checkMillis;
    }

    public void setCheckMillis(long checkMillis) {
        this.checkMillis = checkMillis;
    }

    public long getCheckThreshold() {
        return checkThreshold;
    }

    public void setCheckThreshold(long checkThreshold) {
        this.checkThreshold = checkThreshold;
    }
}
