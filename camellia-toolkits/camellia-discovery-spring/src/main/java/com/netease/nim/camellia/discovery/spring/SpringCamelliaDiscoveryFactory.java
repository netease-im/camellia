package com.netease.nim.camellia.discovery.spring;

import com.netease.nim.camellia.core.discovery.CamelliaDiscovery;
import com.netease.nim.camellia.core.discovery.CamelliaDiscoveryFactory;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by caojiajun on 2026/2/13
 */
public class SpringCamelliaDiscoveryFactory implements CamelliaDiscoveryFactory {

    private final DiscoveryClient discoveryClient;
    private final int intervalSeconds;
    private final ConcurrentHashMap<String, SpringCamelliaDiscovery> map = new ConcurrentHashMap<>();

    public SpringCamelliaDiscoveryFactory(DiscoveryClient discoveryClient) {
        this(discoveryClient, 5);
    }

    public SpringCamelliaDiscoveryFactory(DiscoveryClient discoveryClient, int intervalSeconds) {
        this.discoveryClient = discoveryClient;
        this.intervalSeconds = intervalSeconds;
    }

    @Override
    public CamelliaDiscovery getDiscovery(String serviceName) {
        SpringCamelliaDiscovery discovery = map.get(serviceName);
        if (discovery != null) {
            return discovery;
        }
        return map.computeIfAbsent(serviceName, k -> new SpringCamelliaDiscovery(discoveryClient, serviceName, intervalSeconds));
    }
}
