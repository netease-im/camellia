package com.netease.nim.camellia.redis.proxy.conf;

import java.util.Objects;

/**
 * Created by caojiajun on 2023/8/3
 */
public class MultiTenantConfig implements Comparable<MultiTenantConfig> {
    private long bid;
    private String bgroup;
    private String password;
    private String route;

    public long getBid() {
        return bid;
    }

    public void setBid(long bid) {
        this.bid = bid;
    }

    public String getBgroup() {
        return bgroup;
    }

    public void setBgroup(String bgroup) {
        this.bgroup = bgroup;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRoute() {
        return route;
    }

    public void setRoute(String route) {
        this.route = route;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        MultiTenantConfig config = (MultiTenantConfig) o;
        return bid == config.bid && Objects.equals(bgroup, config.bgroup) && Objects.equals(password, config.password) && Objects.equals(route, config.route);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bid, bgroup, password, route);
    }

    @Override
    public String toString() {
        return "MultiTenantConfig{" +
                "bid=" + bid +
                ", bgroup='" + bgroup + '\'' +
                ", password='" + password + '\'' +
                ", route='" + route + '\'' +
                '}';
    }

    @Override
    public int compareTo(MultiTenantConfig o) {
        return toString().compareTo(o.toString());
    }
}
