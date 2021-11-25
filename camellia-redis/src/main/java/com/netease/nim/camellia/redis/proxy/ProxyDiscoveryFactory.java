package com.netease.nim.camellia.redis.proxy;

import com.netease.nim.camellia.redis.proxy.discovery.common.ProxyDiscovery;

/**
 *
 * Created by caojiajun on 2021/4/13
 */
public interface ProxyDiscoveryFactory {

    ProxyDiscovery getProxyDiscovery(String proxyName);
}
