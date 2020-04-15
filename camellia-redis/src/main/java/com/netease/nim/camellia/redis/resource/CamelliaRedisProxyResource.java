package com.netease.nim.camellia.redis.resource;

import com.netease.nim.camellia.core.model.Resource;

/**
 *
 * Created by caojiajun on 2020/3/6.
 */
public class CamelliaRedisProxyResource extends Resource {
    private String password;
    private String proxyName;

    public CamelliaRedisProxyResource(String password, String proxyName) {
        this.password = password;
        this.proxyName = proxyName;
        StringBuilder url = new StringBuilder();
        url.append(RedisType.CamelliaRedisProxy.getPrefix());
        if (password != null) {
            url.append(password);
        }
        url.append("@");
        url.append(proxyName);
        this.setUrl(url.toString());
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getProxyName() {
        return proxyName;
    }

    public void setProxyName(String proxyName) {
        this.proxyName = proxyName;
    }
}
