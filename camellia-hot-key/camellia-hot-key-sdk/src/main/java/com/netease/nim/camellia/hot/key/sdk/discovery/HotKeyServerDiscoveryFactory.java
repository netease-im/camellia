package com.netease.nim.camellia.hot.key.sdk.discovery;

import com.netease.nim.camellia.core.discovery.CamelliaDiscovery;
import com.netease.nim.camellia.core.discovery.ReloadableDiscoveryFactory;
import com.netease.nim.camellia.hot.key.sdk.netty.HotKeyServerAddr;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by caojiajun on 2023/5/15
 */
public class HotKeyServerDiscoveryFactory extends ReloadableDiscoveryFactory<HotKeyServerAddr>  {

    private final ConcurrentHashMap<String, HotKeyServerDiscovery> map = new ConcurrentHashMap<>();

    public HotKeyServerDiscoveryFactory(NamedServerListGetter<HotKeyServerAddr> getter, int reloadIntervalSeconds, ScheduledExecutorService schedule) {
        super(getter, reloadIntervalSeconds, schedule);
    }

    public HotKeyServerDiscoveryFactory(NamedServerListGetter<HotKeyServerAddr> getter, int reloadIntervalSeconds) {
        super(getter, reloadIntervalSeconds);
    }

    public HotKeyServerDiscoveryFactory(NamedServerListGetter<HotKeyServerAddr> getter) {
        super(getter, 5);
    }

    @Override
    public HotKeyServerDiscovery getDiscovery(String serviceName) {
        HotKeyServerDiscovery discovery = map.get(serviceName);
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


    private HotKeyServerDiscovery init(String serviceName) {
        CamelliaDiscovery<HotKeyServerAddr> discovery = super.getDiscovery(serviceName);
        return new HotKeyServerDiscovery() {
            @Override
            public String getName() {
                return serviceName;
            }

            @Override
            public List<HotKeyServerAddr> findAll() {
                return discovery.findAll();
            }

            @Override
            public void setCallback(CamelliaDiscovery.Callback<HotKeyServerAddr> callback) {
                discovery.setCallback(callback);
            }

            @Override
            public void clearCallback(CamelliaDiscovery.Callback<HotKeyServerAddr> callback) {
                discovery.clearCallback(callback);
            }
        };
    }
}
