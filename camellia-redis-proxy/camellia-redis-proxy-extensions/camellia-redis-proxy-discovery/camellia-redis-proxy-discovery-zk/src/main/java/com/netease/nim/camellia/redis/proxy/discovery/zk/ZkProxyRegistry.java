package com.netease.nim.camellia.redis.proxy.discovery.zk;

import com.netease.nim.camellia.redis.base.proxy.Proxy;
import com.netease.nim.camellia.zk.ZkRegistry;

/**
 *
 * Created by caojiajun on 2020/8/12
 */
public class ZkProxyRegistry extends ZkRegistry<Proxy> {

    public ZkProxyRegistry(String zkUrl, String basePath, String applicationName, Proxy proxy) {
        super(zkUrl, basePath, applicationName, proxy);
    }

    public ZkProxyRegistry(String zkUrl, int sessionTimeoutMs, int connectionTimeoutMs,
                           int baseSleepTimeMs, int maxRetries, String basePath, String applicationName, Proxy proxy) {
        super(zkUrl, sessionTimeoutMs, connectionTimeoutMs, baseSleepTimeMs, maxRetries, basePath, applicationName, proxy);
    }
}
