package com.netease.nim.camellia.redis.proxy.auth;

/**
 * 租户身份标识，id+group
 * Created by caojiajun on 2022/9/15
 */
public class IdentityInfo {
    private final Long bid;
    private final String bgroup;

    public IdentityInfo(Long bid, String bgroup) {
        this.bid = bid;
        this.bgroup = bgroup;
    }

    public Long getBid() {
        return bid;
    }

    public String getBgroup() {
        return bgroup;
    }

    @Override
    public String toString() {
        return "IdentityInfo{" +
                "bid=" + bid +
                ", bgroup='" + bgroup + '\'' +
                '}';
    }
}
