package com.netease.nim.camellia.core.discovery;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by caojiajun on 2022/3/8
 */
public class ReloadableDiscoveryFactory<T> implements CamelliaDiscoveryFactory<T> {

    private final NamedServerListGetter<T> getter;
    private final int reloadIntervalSeconds;
    private final ScheduledExecutorService schedule;

    private final ConcurrentHashMap<String, ReloadableCamelliaDiscovery<T>> map = new ConcurrentHashMap<>();

    public ReloadableDiscoveryFactory(NamedServerListGetter<T> getter, int reloadIntervalSeconds, ScheduledExecutorService schedule) {
        this.getter = getter;
        this.reloadIntervalSeconds = reloadIntervalSeconds;
        this.schedule = schedule;
    }

    public ReloadableDiscoveryFactory(NamedServerListGetter<T> getter, int reloadIntervalSeconds) {
        this.getter = getter;
        this.reloadIntervalSeconds = reloadIntervalSeconds;
        this.schedule = null;
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
                    discovery = new ReloadableCamelliaDiscovery<>(() -> getter.findAll(serviceName), reloadIntervalSeconds, schedule);
                    map.put(serviceName, discovery);
                }
            }
        }
        return discovery;
    }
}
