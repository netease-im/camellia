package com.netease.nim.camellia.redis.base.resource;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.redis.base.resource.RedisType;

/**
 * 格式如下：
 * 1、没有密码
 * redis-proxies-discovery://@proxyName
 * 2、有密码
 * redis-proxies-discovery://passwd@proxyName
 * 3、有密码且有账号
 * redis-proxies-discovery://username:passwd@proxyName
 */
public class RedisProxiesDiscoveryResource extends Resource {

    private final String userName;
    private final String password;
    private final String proxyName;

    public RedisProxiesDiscoveryResource(String userName, String password, String proxyName) {
        this.userName = userName;
        this.password = password;
        this.proxyName = proxyName;
        StringBuilder url = new StringBuilder();
        url.append(RedisType.RedisProxiesDiscovery.getPrefix());
        if (userName != null && password != null) {
            url.append(userName).append(":").append(password);
        } else if (userName == null && password != null) {
            url.append(password);
        }
        url.append("@");
        url.append(proxyName);
        this.setUrl(url.toString());
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
}
