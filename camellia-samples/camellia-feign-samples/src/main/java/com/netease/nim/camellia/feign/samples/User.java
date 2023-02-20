package com.netease.nim.camellia.feign.samples;

import com.netease.nim.camellia.core.client.annotation.LoadBalanceKey;
import com.netease.nim.camellia.core.client.annotation.RouteKey;

/**
 * Created by caojiajun on 2022/3/30
 */
public class User {

    @RouteKey
    private long tenantId;

    @LoadBalanceKey
    private long uid;

    private String name;
    private String ext;

    public long getTenantId() {
        return tenantId;
    }

    public void setTenantId(long tenantId) {
        this.tenantId = tenantId;
    }

    public long getUid() {
        return uid;
    }

    public void setUid(long uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getExt() {
        return ext;
    }

    public void setExt(String ext) {
        this.ext = ext;
    }
}
