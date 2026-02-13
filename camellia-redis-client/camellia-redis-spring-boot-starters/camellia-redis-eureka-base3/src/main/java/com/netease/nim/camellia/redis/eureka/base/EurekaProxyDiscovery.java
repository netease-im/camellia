package com.netease.nim.camellia.redis.eureka.base;

import com.netease.nim.camellia.core.discovery.ReloadableCamelliaDiscovery;
import com.netease.nim.camellia.core.discovery.ServerNode;
import com.netflix.appinfo.InstanceInfo;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.eureka.EurekaServiceInstance;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * Created by caojiajun on 2019/11/26.
 */
public class EurekaProxyDiscovery extends ReloadableCamelliaDiscovery {

    private final String applicationName;
    private final DiscoveryClient discoveryClient;

    public EurekaProxyDiscovery(DiscoveryClient discoveryClient, String applicationName, int refreshIntervalSeconds) {
        this.discoveryClient = discoveryClient;
        this.applicationName = applicationName;
        init(() -> new ArrayList<>(refreshProxySet()), refreshIntervalSeconds);
    }

    @Override
    public List<ServerNode> findAll() {
        return new ArrayList<>(refreshProxySet());
    }

    private Set<ServerNode> refreshProxySet() {
        List<ServiceInstance> instances = discoveryClient.getInstances(applicationName);
        Set<ServerNode> proxySet = new HashSet<>();
        for (ServiceInstance instance : instances) {
            try {
                if (instance instanceof EurekaServiceInstance) {
                    InstanceInfo instanceInfo = ((EurekaServiceInstance) instance).getInstanceInfo();
                    if (instanceInfo.getStatus() != InstanceInfo.InstanceStatus.UP) continue;
                    String ipAddr = instanceInfo.getIPAddr();
                    int port = instanceInfo.getPort();
                    ServerNode proxy = new ServerNode(ipAddr, port);
                    proxySet.add(proxy);
                } else {
                    ServerNode proxy = new ServerNode(instance.getHost(), instance.getPort());
                    proxySet.add(proxy);
                }
            } catch (Throwable e) {
                ServerNode proxy = new ServerNode(instance.getHost(), instance.getPort());
                proxySet.add(proxy);
            }
        }
        return proxySet;
    }
}
