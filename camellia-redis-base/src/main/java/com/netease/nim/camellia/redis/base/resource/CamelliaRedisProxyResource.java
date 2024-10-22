package com.netease.nim.camellia.redis.base.resource;

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
    private final int db;

    public CamelliaRedisProxyResource(String password, String proxyName) {
        this(password, proxyName, -1, "default");
    }

    public CamelliaRedisProxyResource(String password, String proxyName, long bid, String bgroup) {
        this(password, proxyName, bid, bgroup, 0);
    }

    public CamelliaRedisProxyResource(String password, String proxyName, long bid, String bgroup, int db) {
        this.password = password;
        this.proxyName = proxyName;
        this.bid = bid;
        this.bgroup = bgroup;
        this.db = db;
        StringBuilder url = new StringBuilder();
        url.append(RedisType.CamelliaRedisProxy.getPrefix());
        if (password != null) {
            url.append(password);
        }
        url.append("@");
        url.append(proxyName);
        url.append("?");
        if (bid > 0 && bgroup != null) {
            url.append("bid=").append(bid).append("&");
            url.append("bgroup=").append(bgroup).append("&");
        }
        if (db > 0) {
            url.append("db=").append(db);
        }
        if (url.charAt(url.length() - 1) == '?' || url.charAt(url.length() - 1) == '&') {
            url.deleteCharAt(url.length() - 1);
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

    public int getDb() {
        return db;
    }
}
