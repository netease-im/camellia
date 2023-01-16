package com.netease.nim.camellia.delayqueue.sdk.api;

import com.netease.nim.camellia.core.discovery.CamelliaDiscovery;
import com.netease.nim.camellia.core.discovery.ReloadableDiscoveryFactory;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by caojiajun on 2022/10/9
 */
public class DelayQueueServerDiscoveryFactory extends ReloadableDiscoveryFactory<DelayQueueServer> {

    private final ConcurrentHashMap<String, DelayQueueServerDiscovery> map = new ConcurrentHashMap<>();

    public DelayQueueServerDiscoveryFactory(NamedServerListGetter<DelayQueueServer> getter, int reloadIntervalSeconds, ScheduledExecutorService schedule) {
        super(getter, reloadIntervalSeconds, schedule);
    }

    public DelayQueueServerDiscoveryFactory(NamedServerListGetter<DelayQueueServer> getter, int reloadIntervalSeconds) {
        super(getter, reloadIntervalSeconds);
    }

    public DelayQueueServerDiscoveryFactory(NamedServerListGetter<DelayQueueServer> getter) {
        super(getter, 5);
    }

    @Override
    public DelayQueueServerDiscovery getDiscovery(String serviceName) {
        DelayQueueServerDiscovery discovery = map.get(serviceName);
        if (discovery == null) {
            synchronized (map) {
                discovery = map.get(serviceName);
                if (discovery == null) {
                    discovery = init(serviceName);
                    map.put(serviceName, discovery);
                }
            }
        }
        return discovery;
    }

    private DelayQueueServerDiscovery init(String serviceName) {
        CamelliaDiscovery<DelayQueueServer> discovery = super.getDiscovery(serviceName);
        return new DelayQueueServerDiscovery() {
            @Override
            public List<DelayQueueServer> findAll() {
                return discovery.findAll();
            }

            @Override
            public void setCallback(Callback<DelayQueueServer> callback) {
                discovery.setCallback(callback);
            }

            @Override
            public void clearCallback(Callback<DelayQueueServer> callback) {
                discovery.clearCallback(callback);
            }
        };
    }
}
