package com.netease.nim.camellia.redis.resource;

import com.netease.nim.camellia.core.model.Resource;

/**
 *
 * Created by caojiajun on 2020/3/6.
 */
public class CamelliaRedisProxyResource extends Resource {
    private final String password;
    private final String proxyName;
    private final long bid;
    private final String bgroup;

    public CamelliaRedisProxyResource(String password, String proxyName) {
        this(password, proxyName, -1, "default");
    }

    public CamelliaRedisProxyResource(String password, String proxyName, long bid, String bgroup) {
        this.password = password;
        this.proxyName = proxyName;
        this.bid = bid;
        this.bgroup = bgroup;
        StringBuilder url = new StringBuilder();
        url.append(RedisType.CamelliaRedisProxy.getPrefix());
        if (password != null) {
            url.append(password);
        }
        url.append("@");
        url.append(proxyName);
        if (bid > 0 && bgroup != null) {
            url.append("?");
            url.append("bid=").append(bid).append("&");
            url.append("bgroup=").append(bgroup);
        }
        this.setUrl(url.toString());
    }

    public long getBid() {
        return bid;
    }

    public String getBgroup() {
        return bgroup;
    }

    public String getPassword() {
        return password;
    }

    public String getProxyName() {
        return proxyName;
    }

}
