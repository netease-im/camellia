package com.netease.nim.camellia.redis.zk.discovery.springboot;

import com.netease.nim.camellia.redis.proxy.ProxyDiscoveryFactory;
import com.netease.nim.camellia.redis.proxy.discovery.common.IProxyDiscovery;
import com.netease.nim.camellia.redis.proxy.discovery.zk.ZkClientFactory;
import com.netease.nim.camellia.redis.proxy.discovery.zk.ZkProxyDiscovery;

import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * Created by caojiajun on 2021/4/13
 */
public class ZkProxyDiscoveryFactory implements ProxyDiscoveryFactory {

    private final ConcurrentHashMap<String, IProxyDiscovery> proxyDiscoveryMap = new ConcurrentHashMap<>();

    private final CamelliaRedisZkDiscoveryProperties properties;
    private final ZkClientFactory factory;

    public ZkProxyDiscoveryFactory(CamelliaRedisZkDiscoveryProperties properties) {
        this.properties = properties;
        this.factory = new ZkClientFactory(properties.getSessionTimeoutMs(), properties.getConnectionTimeoutMs(),
                properties.getBaseSleepTimeMs(), properties.getMaxRetries());
    }

    public IProxyDiscovery getProxyDiscovery(String proxyName) {
        IProxyDiscovery proxyDiscovery = proxyDiscoveryMap.get(proxyName);
        if (proxyDiscovery == null) {
            synchronized (proxyDiscoveryMap) {
                proxyDiscovery = proxyDiscoveryMap.get(proxyName);
                if (proxyDiscovery == null) {
                    proxyDiscovery = new ZkProxyDiscovery(factory, properties.getZkUrl(),
                            properties.getBasePath(), proxyName, properties.getReloadIntervalSeconds());
                    proxyDiscoveryMap.putIfAbsent(proxyName, proxyDiscovery);
                }
            }
        }
        return proxyDiscovery;
    }
}

