package com.netease.nim.camellia.redis.proxy.auth;

/**
 * 租户身份标识，bid+group
 * Created by caojiajun on 2022/9/15
 */
public record IdentityInfo(Long bid, String bgroup) {

    @Override
    public String toString() {
        return "IdentityInfo{" +
                "bid=" + bid +
                ", bgroup='" + bgroup + '\'' +
                '}';
    }
}
