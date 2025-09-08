package com.netease.nim.camellia.redis.proxy.conf;

import java.util.Objects;

/**
 * Created by caojiajun on 2023/8/3
 */
public class MultiTenantSimpleConfig {
    private String name;
    private String password;
    private String biz;

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

    public String getBiz() {
        return biz;
    }

    public void setBiz(String biz) {
        this.biz = biz;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        MultiTenantSimpleConfig that = (MultiTenantSimpleConfig) o;
        return Objects.equals(name, that.name) && Objects.equals(password, that.password) && Objects.equals(biz, that.biz);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, password, biz);
    }
}
