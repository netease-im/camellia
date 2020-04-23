package com.netease.nim.camellia.redis.eureka.springboot;

import com.netease.nim.camellia.core.util.CamelliaThreadFactory;
import com.netease.nim.camellia.redis.proxy.Proxy;
import com.netease.nim.camellia.redis.proxy.ProxyDiscovery;
import com.netflix.appinfo.InstanceInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.eureka.EurekaDiscoveryClient;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 *
 * Created by yuanyuanjun on 2019/11/26.
 */
public class EurekaProxyDiscovery extends ProxyDiscovery {

    private static final Logger logger = LoggerFactory.getLogger(EurekaProxyDiscovery.class);

    private String applicationName;
    private Set<Proxy> proxySet;
    private DiscoveryClient discoveryClient;

    public EurekaProxyDiscovery(DiscoveryClient discoveryClient, String applicationName, int refreshInervalSeconds) {
        this.discoveryClient = discoveryClient;
        this.applicationName = applicationName;
        this.proxySet = refreshProxySet();
        Executors.newSingleThreadScheduledExecutor(new CamelliaThreadFactory(EurekaProxyDiscovery.class))
                .scheduleAtFixedRate(new RefreshThread(this), refreshInervalSeconds, refreshInervalSeconds, TimeUnit.SECONDS);
    }

    @Override
    public List<Proxy> findAll() {
        return new ArrayList<>(refreshProxySet());
    }

    private Set<Proxy> refreshProxySet() {
        List<ServiceInstance> instances = discoveryClient.getInstances(applicationName);
        Set<Proxy> proxySet = new HashSet<>();
        for (ServiceInstance instance : instances) {
            if (instance instanceof EurekaDiscoveryClient.EurekaServiceInstance) {
                InstanceInfo instanceInfo = ((EurekaDiscoveryClient.EurekaServiceInstance) instance).getInstanceInfo();
                if (instanceInfo.getStatus() != InstanceInfo.InstanceStatus.UP) continue;
                String ipAddr = instanceInfo.getIPAddr();
                int port = instanceInfo.getPort();
                Proxy proxy = new Proxy();
                proxy.setHost(ipAddr);
                proxy.setPort(port);
                proxySet.add(proxy);
            }
        }
        return proxySet;
    }

    private static class RefreshThread implements Runnable {

        private EurekaProxyDiscovery discovery;

        RefreshThread(EurekaProxyDiscovery discovery) {
            this.discovery = discovery;
        }

        @Override
        public void run() {
            try {
                Set<Proxy> newProxySet = discovery.refreshProxySet();
                Set<Proxy> oldProxySet = discovery.proxySet;

                //new - old = add
                Set<Proxy> addSet = new HashSet<>(newProxySet);
                addSet.removeAll(oldProxySet);
                if (!addSet.isEmpty()) {
                    //callback add
                    for (Proxy proxy : addSet) {
                        try {
                            discovery.invokeAddProxyCallback(proxy);
                        } catch (Exception e) {
                            logger.error("callback add error", e);
                        }
                    }
                }

                //old - new = remove
                Set<Proxy> removeSet = new HashSet<>(oldProxySet);
                removeSet.removeAll(newProxySet);
                if (!removeSet.isEmpty()) {
                    //callback remove
                    for (Proxy proxy : removeSet) {
                        try {
                            discovery.invokeRemoveProxyCallback(proxy);
                        } catch (Exception e) {
                            logger.error("callback remove error", e);
                        }
                    }
                }

                discovery.proxySet = newProxySet;
            } catch (Exception e) {
                logger.error("RefreshThread error", e);
            }
        }
    }
}
