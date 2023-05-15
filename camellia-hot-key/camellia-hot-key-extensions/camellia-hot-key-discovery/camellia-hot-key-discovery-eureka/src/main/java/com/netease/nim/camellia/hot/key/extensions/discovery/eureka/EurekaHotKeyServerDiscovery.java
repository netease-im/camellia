package com.netease.nim.camellia.hot.key.extensions.discovery.eureka;

import com.netease.nim.camellia.core.discovery.ReloadableCamelliaDiscovery;
import com.netease.nim.camellia.hot.key.sdk.discovery.HotKeyServerDiscovery;
import com.netease.nim.camellia.hot.key.sdk.netty.HotKeyServerAddr;
import com.netflix.appinfo.InstanceInfo;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.eureka.EurekaDiscoveryClient;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * Created by caojiajun on 2023/5/15
 */
public class EurekaHotKeyServerDiscovery extends ReloadableCamelliaDiscovery<HotKeyServerAddr> implements HotKeyServerDiscovery {

    private final DiscoveryClient discoveryClient;
    private final String applicationName;

    public EurekaHotKeyServerDiscovery(DiscoveryClient discoveryClient, String applicationName, int refreshIntervalSeconds) {
        this.discoveryClient = discoveryClient;
        this.applicationName = applicationName;
        init(() -> new ArrayList<>(refreshProxySet()), refreshIntervalSeconds);
    }

    private Set<HotKeyServerAddr> refreshProxySet() {
        List<ServiceInstance> instances = discoveryClient.getInstances(applicationName);
        Set<HotKeyServerAddr> addrs = new HashSet<>();
        for (ServiceInstance instance : instances) {
            if (instance instanceof EurekaDiscoveryClient.EurekaServiceInstance) {
                InstanceInfo instanceInfo = ((EurekaDiscoveryClient.EurekaServiceInstance) instance).getInstanceInfo();
                if (instanceInfo.getStatus() != InstanceInfo.InstanceStatus.UP) continue;
                String ipAddr = instanceInfo.getIPAddr();
                int port = instanceInfo.getPort();
                HotKeyServerAddr proxy = new HotKeyServerAddr(ipAddr, port);
                addrs.add(proxy);
            }
        }
        return addrs;
    }

    @Override
    public String getName() {
        return applicationName;
    }
}
