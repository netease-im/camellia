package com.netease.nim.camellia.core.discovery;


/**
 * Created by caojiajun on 2026/2/13
 */
public class LocalConfCamelliaDiscoveryFactory implements CamelliaDiscoveryFactory {

    private final String name;
    private final LocalConfCamelliaDiscovery discovery;

    public LocalConfCamelliaDiscoveryFactory(String name, LocalConfCamelliaDiscovery discovery) {
        this.name = name;
        this.discovery = discovery;
    }

    @Override
    public CamelliaDiscovery getDiscovery(String serviceName) {
        if (serviceName.equals(name)) {
            return discovery;
        }
        return null;
    }
}
