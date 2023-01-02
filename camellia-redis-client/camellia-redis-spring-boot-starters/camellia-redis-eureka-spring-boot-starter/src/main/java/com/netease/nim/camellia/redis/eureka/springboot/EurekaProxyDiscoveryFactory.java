package com.netease.nim.camellia.redis.eureka.springboot;

import com.netease.nim.camellia.redis.eureka.base.EurekaProxyDiscovery;
import com.netease.nim.camellia.redis.proxy.ProxyDiscoveryFactory;
import com.netease.nim.camellia.redis.proxy.discovery.common.IProxyDiscovery;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * Created by caojiajun on 2021/4/13
 */
public class EurekaProxyDiscoveryFactory implements ProxyDiscoveryFactory {

    private final DiscoveryClient discoveryClient;
    private final ConcurrentHashMap<String, IProxyDiscovery> proxyDiscoveryMap = new ConcurrentHashMap<>();
    private final int refreshIntervalSeconds;

    public EurekaProxyDiscoveryFactory(DiscoveryClient discoveryClient, int refreshIntervalSeconds) {
        this.discoveryClient = discoveryClient;
        this.refreshIntervalSeconds = refreshIntervalSeconds;
    }

    @Override
    public IProxyDiscovery getProxyDiscovery(String proxyName) {
        IProxyDiscovery proxyDiscovery = proxyDiscoveryMap.get(proxyName);
        if (proxyDiscovery == null) {
            synchronized (proxyDiscoveryMap) {
                proxyDiscovery = proxyDiscoveryMap.get(proxyName);
                if (proxyDiscovery == null) {
                    proxyDiscovery = new EurekaProxyDiscovery(discoveryClient, proxyName, refreshIntervalSeconds);
                    proxyDiscoveryMap.putIfAbsent(proxyName, proxyDiscovery);
                }
            }
        }
        return proxyDiscovery;
    }
}
