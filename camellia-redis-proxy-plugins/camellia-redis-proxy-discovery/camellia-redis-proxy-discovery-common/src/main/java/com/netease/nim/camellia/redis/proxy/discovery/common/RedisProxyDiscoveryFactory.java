package com.netease.nim.camellia.redis.proxy.discovery.common;

import com.netease.nim.camellia.core.discovery.CamelliaDiscovery;
import com.netease.nim.camellia.core.discovery.ReloadableDiscoveryFactory;

import java.util.List;

/**
 * Created by caojiajun on 2022/3/8
 */
public class RedisProxyDiscoveryFactory extends ReloadableDiscoveryFactory<Proxy> {

    public RedisProxyDiscoveryFactory(NamedServerListGetter<Proxy> getter, int reloadIntervalSeconds) {
        super(getter, reloadIntervalSeconds);
    }

    public RedisProxyDiscoveryFactory(NamedServerListGetter<Proxy> getter) {
        super(getter, 5);
    }

    @Override
    public IProxyDiscovery getDiscovery(String serviceName) {
        final CamelliaDiscovery<Proxy> discovery = super.getDiscovery(serviceName);
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
