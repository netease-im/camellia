package com.netease.nim.camellia.redis.proxy;

import com.netease.nim.camellia.redis.proxy.discovery.common.IProxyDiscovery;

/**
 *
 * Created by caojiajun on 2021/4/13
 */
public interface ProxyDiscoveryFactory {

    IProxyDiscovery getProxyDiscovery(String proxyName);
}
