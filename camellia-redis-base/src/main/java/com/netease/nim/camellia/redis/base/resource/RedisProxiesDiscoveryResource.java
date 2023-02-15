package com.netease.nim.camellia.redis.base.resource;

import com.netease.nim.camellia.core.model.Resource;

/**
 * 格式如下：
 * 1、没有密码
 * redis-proxies-discovery://@proxyName
 * 2、有密码
 * redis-proxies-discovery://passwd@proxyName
 * 3、有密码且有账号
 * redis-proxies-discovery://username:passwd@proxyName
 * 4、有db
 * redis-proxies-discovery://username:passwd@proxyName?db=1
 */
public class RedisProxiesDiscoveryResource extends Resource {

    private final String userName;
    private final String password;
    private final String proxyName;
    private final int db;

    public RedisProxiesDiscoveryResource(String userName, String password, String proxyName, int db) {
        this.userName = userName;
        this.password = password;
        this.proxyName = proxyName;
        this.db = db;
        StringBuilder url = new StringBuilder();
        url.append(RedisType.RedisProxiesDiscovery.getPrefix());
        if (userName != null && password != null) {
            url.append(userName).append(":").append(password);
        } else if (userName == null && password != null) {
            url.append(password);
        }
        url.append("@");
        url.append(proxyName);
        if (db > 0) {
            url.append("?db=").append(db);
        }
        this.setUrl(url.toString());
    }

    public RedisProxiesDiscoveryResource(String userName, String password, String proxyName) {
        this(userName, password, proxyName, 0);
    }

    public String getUserName() {
        return userName;
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
