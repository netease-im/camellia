package com.netease.nim.camellia.core.discovery;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by caojiajun on 2026/2/25
 */
public class CamelliaDiscoveryFactorySwitcher implements CamelliaDiscoveryFactory {

    private final List<CamelliaDiscoveryFactory> factoryList;
    private final DiscoveryIndexSwitcher switcher;

    public CamelliaDiscoveryFactorySwitcher(List<CamelliaDiscoveryFactory> factoryList, DiscoveryIndexSwitcher switcher) {
        this.factoryList = factoryList;
        this.switcher = switcher;
    }

    @Override
    public CamelliaDiscovery getDiscovery(String serviceName) {
        List<CamelliaDiscovery> discoveryList = new ArrayList<>();
        for (CamelliaDiscoveryFactory factory : factoryList) {
            CamelliaDiscovery discovery = factory.getDiscovery(serviceName);
            discoveryList.add(discovery);
        }
        return new CamelliaDiscoverySwitcher(discoveryList, switcher);
    }
}
