package com.netease.nim.camellia.core.discovery;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by caojiajun on 2022/3/8
 */
public class ReloadableDiscoveryFactory implements CamelliaDiscoveryFactory {

    private final NamedServerListGetter getter;
    private final int reloadIntervalSeconds;
    private final ScheduledExecutorService schedule;

    private final ConcurrentHashMap<String, ReloadableCamelliaDiscovery> map = new ConcurrentHashMap<>();

    public ReloadableDiscoveryFactory(NamedServerListGetter getter, int reloadIntervalSeconds, ScheduledExecutorService schedule) {
        this.getter = getter;
        this.reloadIntervalSeconds = reloadIntervalSeconds;
        this.schedule = schedule;
    }

    public ReloadableDiscoveryFactory(NamedServerListGetter getter, int reloadIntervalSeconds) {
        this.getter = getter;
        this.reloadIntervalSeconds = reloadIntervalSeconds;
        this.schedule = null;
    }

    public ReloadableDiscoveryFactory(NamedServerListGetter getter) {
        this(getter, 5);
    }

    public static interface NamedServerListGetter {
        List<ServerNode> findAll(String serviceName);
    }

    @Override
    public CamelliaDiscovery getDiscovery(final String serviceName) {
        ReloadableCamelliaDiscovery discovery = map.get(serviceName);
        if (discovery == null) {
            synchronized (map) {
                discovery = map.get(serviceName);
                if (discovery == null) {
                    discovery = new ReloadableCamelliaDiscovery(() -> getter.findAll(serviceName), reloadIntervalSeconds, schedule);
                    map.put(serviceName, discovery);
                }
            }
        }
        return discovery;
    }
}
