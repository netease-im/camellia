package com.netease.nim.camellia.redis.proxy.plugin.rewrite;

import java.util.Objects;

/**
 * Created by caojiajun on 2023/10/7
 */
public class RouteRewriteResult {

    private final long bid;
    private final String bgroup;

    public RouteRewriteResult(long bid, String bgroup) {
        this.bid = bid;
        this.bgroup = bgroup;
    }

    public long getBid() {
        return bid;
    }

    public String getBgroup() {
        return bgroup;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RouteRewriteResult that = (RouteRewriteResult) o;
        return bid == that.bid && Objects.equals(bgroup, that.bgroup);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bid, bgroup);
    }
}
