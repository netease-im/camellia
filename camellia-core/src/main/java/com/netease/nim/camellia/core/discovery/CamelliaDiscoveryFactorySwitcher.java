package com.netease.nim.camellia.core.discovery;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by caojiajun on 2026/2/25
 */
public class CamelliaDiscoveryFactorySwitcher implements CamelliaDiscoveryFactory {

    private final List<CamelliaDiscoveryFactory> factoryList;
    private final DiscoveryIndexSwitcher switcher;
    private final ScheduledExecutorService scheduler;
    private final int scheduleIntervalSeconds;
    private final ConcurrentHashMap<String, CamelliaDiscovery> map = new ConcurrentHashMap<>();

    public CamelliaDiscoveryFactorySwitcher(List<CamelliaDiscoveryFactory> factoryList, DiscoveryIndexSwitcher switcher) {
        this(factoryList, switcher, null, 10);
    }

    public CamelliaDiscoveryFactorySwitcher(List<CamelliaDiscoveryFactory> factoryList, DiscoveryIndexSwitcher switcher,
                                            ScheduledExecutorService scheduler, int scheduleIntervalSeconds) {
        this.factoryList = factoryList;
        this.switcher = switcher;
        this.scheduler = scheduler;
        this.scheduleIntervalSeconds = scheduleIntervalSeconds;
    }

    @Override
    public CamelliaDiscovery getDiscovery(String serviceName) {
        CamelliaDiscovery discovery = map.get(serviceName);
        if (discovery != null) {
            return discovery;
        }
        return map.computeIfAbsent(serviceName, this::createDiscovery);
    }

    private CamelliaDiscovery createDiscovery(String serviceName) {
        List<CamelliaDiscovery> discoveryList = new ArrayList<>(factoryList.size());
        for (CamelliaDiscoveryFactory factory : factoryList) {
            CamelliaDiscovery discovery = factory.getDiscovery(serviceName);
            discoveryList.add(discovery);
        }
        if (scheduler == null) {
            return new CamelliaDiscoverySwitcher(discoveryList, switcher);
        }
        return new CamelliaDiscoverySwitcher(discoveryList, switcher, scheduler, scheduleIntervalSeconds);
    }
}
