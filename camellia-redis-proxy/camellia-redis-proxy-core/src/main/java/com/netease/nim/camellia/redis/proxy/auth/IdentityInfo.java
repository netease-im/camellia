package com.netease.nim.camellia.redis.proxy.auth;

import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        IdentityInfo that = (IdentityInfo) o;
        return Objects.equals(bid, that.bid) && Objects.equals(bgroup, that.bgroup);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bid, bgroup);
    }
}
