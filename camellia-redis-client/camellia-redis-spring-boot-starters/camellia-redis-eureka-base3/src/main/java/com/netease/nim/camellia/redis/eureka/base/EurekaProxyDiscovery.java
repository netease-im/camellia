package com.netease.nim.camellia.redis.eureka.base;

import com.netease.nim.camellia.core.discovery.ReloadableCamelliaDiscovery;
import com.netease.nim.camellia.redis.base.proxy.IProxyDiscovery;
import com.netease.nim.camellia.redis.base.proxy.Proxy;
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
public class EurekaProxyDiscovery extends ReloadableCamelliaDiscovery<Proxy> implements IProxyDiscovery {

    private final String applicationName;
    private final DiscoveryClient discoveryClient;

    public EurekaProxyDiscovery(DiscoveryClient discoveryClient, String applicationName, int refreshIntervalSeconds) {
        this.discoveryClient = discoveryClient;
        this.applicationName = applicationName;
        init(() -> new ArrayList<>(refreshProxySet()), refreshIntervalSeconds);
    }

    @Override
    public List<Proxy> findAll() {
        return new ArrayList<>(refreshProxySet());
    }

    private Set<Proxy> refreshProxySet() {
        List<ServiceInstance> instances = discoveryClient.getInstances(applicationName);
        Set<Proxy> proxySet = new HashSet<>();
        for (ServiceInstance instance : instances) {
            try {
                if (instance instanceof EurekaServiceInstance) {
                    InstanceInfo instanceInfo = ((EurekaServiceInstance) instance).getInstanceInfo();
                    if (instanceInfo.getStatus() != InstanceInfo.InstanceStatus.UP) continue;
                    String ipAddr = instanceInfo.getIPAddr();
                    int port = instanceInfo.getPort();
                    Proxy proxy = new Proxy(ipAddr, port);
                    proxySet.add(proxy);
                } else {
                    Proxy proxy = new Proxy(instance.getHost(), instance.getPort());
                    proxySet.add(proxy);
                }
            } catch (Throwable e) {
                Proxy proxy = new Proxy(instance.getHost(), instance.getPort());
                proxySet.add(proxy);
            }
        }
        return proxySet;
    }
}
