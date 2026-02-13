package com.netease.nim.camellia.core.discovery;

/**
 * Created by caojiajun on 2022/3/2
 */
public interface CamelliaDiscoveryFactory {

    CamelliaDiscovery getDiscovery(String serviceName);

}
