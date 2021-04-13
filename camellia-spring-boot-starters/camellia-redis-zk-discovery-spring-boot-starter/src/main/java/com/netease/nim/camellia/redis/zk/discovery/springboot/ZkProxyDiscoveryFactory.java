package com.netease.nim.camellia.redis.zk.discovery.springboot;

import com.netease.nim.camellia.redis.proxy.ProxyDiscovery;
import com.netease.nim.camellia.redis.proxy.ProxyDiscoveryFactory;
import com.netease.nim.camellia.redis.zk.discovery.ZkClientFactory;
import com.netease.nim.camellia.redis.zk.discovery.ZkProxyDiscovery;

import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * Created by caojiajun on 2021/4/13
 */
public class ZkProxyDiscoveryFactory implements ProxyDiscoveryFactory {

    private final ConcurrentHashMap<String, ProxyDiscovery> proxyDiscoveryMap = new ConcurrentHashMap<>();

    private final CamelliaRedisZkDiscoveryProperties properties;
    private final ZkClientFactory factory;

    public ZkProxyDiscoveryFactory(CamelliaRedisZkDiscoveryProperties properties) {
        this.properties = properties;
        this.factory = new ZkClientFactory(properties.getSessionTimeoutMs(), properties.getConnectionTimeoutMs(),
                properties.getBaseSleepTimeMs(), properties.getMaxRetries());
    }

    public ProxyDiscovery getProxyDiscovery(String proxyName) {
        ProxyDiscovery proxyDiscovery = proxyDiscoveryMap.get(proxyName);
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

