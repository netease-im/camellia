package com.netease.nim.camellia.redis.base.proxy;


/**
 *
 * Created by caojiajun on 2021/4/13
 */
public interface ProxyDiscoveryFactory {

    IProxyDiscovery getProxyDiscovery(String proxyName);
}
