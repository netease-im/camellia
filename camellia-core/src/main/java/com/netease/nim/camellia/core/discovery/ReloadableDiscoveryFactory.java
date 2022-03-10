package com.netease.nim.camellia.core.discovery;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by caojiajun on 2022/3/8
 */
public class ReloadableDiscoveryFactory<T> implements CamelliaDiscoveryFactory<T> {

    private final NamedServerListGetter<T> getter;
    private final int reloadIntervalSeconds;

    private final ConcurrentHashMap<String, ReloadableCamelliaDiscovery<T>> map = new ConcurrentHashMap<>();

    public ReloadableDiscoveryFactory(NamedServerListGetter<T> getter, int reloadIntervalSeconds) {
        this.getter = getter;
        this.reloadIntervalSeconds = reloadIntervalSeconds;
    }

    public ReloadableDiscoveryFactory(NamedServerListGetter<T> getter) {
        this(getter, 5);
    }

    public static interface NamedServerListGetter<T> {
        List<T> findAll(String serviceName);
    }

    @Override
    public CamelliaDiscovery<T> getDiscovery(final String serviceName) {
        ReloadableCamelliaDiscovery<T> discovery = map.get(serviceName);
        if (discovery == null) {
            synchronized (map) {
                discovery = map.get(serviceName);
                if (discovery == null) {
                    discovery = new ReloadableCamelliaDiscovery<>(new ReloadableCamelliaDiscovery.ServerListGetter<T>() {
                        @Override
                        public List<T> findAll() {
                            return getter.findAll(serviceName);
                        }
                    }, reloadIntervalSeconds);
                    map.put(serviceName, discovery);
                }
            }
        }
        return discovery;
    }
}
