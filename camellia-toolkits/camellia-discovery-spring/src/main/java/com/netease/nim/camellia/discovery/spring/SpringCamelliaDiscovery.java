package com.netease.nim.camellia.discovery.spring;

import com.netease.nim.camellia.core.discovery.ReloadableCamelliaDiscovery;
import com.netease.nim.camellia.core.discovery.ServerNode;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by caojiajun on 2026/2/13
 */
public class SpringCamelliaDiscovery extends ReloadableCamelliaDiscovery {

    public SpringCamelliaDiscovery(DiscoveryClient discoveryClient, String applicationName) {
        this(discoveryClient, applicationName, 5);
    }

    public SpringCamelliaDiscovery(DiscoveryClient discoveryClient, String applicationName, int intervalSeconds) {
        super(() -> {
            List<ServiceInstance> instances = discoveryClient.getInstances(applicationName);
            Set<ServerNode> set = new HashSet<>();
            for (ServiceInstance instance : instances) {
                ServerNode node = new ServerNode(instance.getHost(), instance.getPort());
                set.add(node);
            }
            return new ArrayList<>(set);
        }, intervalSeconds);
    }
}
