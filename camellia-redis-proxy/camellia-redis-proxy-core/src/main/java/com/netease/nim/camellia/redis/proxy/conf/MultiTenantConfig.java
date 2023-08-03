package com.netease.nim.camellia.redis.proxy.conf;

/**
 * Created by caojiajun on 2023/8/3
 */
public class MultiTenantConfig {
    private String name;
    private String password;
    private String route;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
}
