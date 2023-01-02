package com.netease.nim.camellia.redis.proxy.discovery.common;

import com.netease.nim.camellia.core.discovery.CamelliaDiscovery;
import com.netease.nim.camellia.core.discovery.ReloadableDiscoveryFactory;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by caojiajun on 2022/3/8
 */
public class RedisProxyDiscoveryFactory extends ReloadableDiscoveryFactory<Proxy> {

    private final ConcurrentHashMap<String, IProxyDiscovery> map = new ConcurrentHashMap<>();

    public RedisProxyDiscoveryFactory(NamedServerListGetter<Proxy> getter, int reloadIntervalSeconds) {
        super(getter, reloadIntervalSeconds);
    }

    public RedisProxyDiscoveryFactory(NamedServerListGetter<Proxy> getter) {
        super(getter, 5);
    }

    @Override
    public IProxyDiscovery getDiscovery(String serviceName) {
        IProxyDiscovery discovery = map.get(serviceName);
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

    private IProxyDiscovery init(String serviceName) {
        CamelliaDiscovery<Proxy> discovery = super.getDiscovery(serviceName);
        return new IProxyDiscovery() {
            @Override
            public List<Proxy> findAll() {
                return discovery.findAll();
            }

            @Override
            public void setCallback(Callback<Proxy> callback) {
                discovery.setCallback(callback);
            }

            @Override
            public void clearCallback(Callback<Proxy> callback) {
                discovery.clearCallback(callback);
            }
        };
    }
}
