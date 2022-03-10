package com.netease.nim.camellia.id.gen.sdk;

import com.netease.nim.camellia.core.discovery.CamelliaDiscovery;
import com.netease.nim.camellia.core.discovery.ReloadableDiscoveryFactory;

import java.util.List;

/**
 * Created by caojiajun on 2022/3/8
 */
public class IdGenServerDiscoveryFactory extends ReloadableDiscoveryFactory<IdGenServer> {

    public IdGenServerDiscoveryFactory(NamedServerListGetter<IdGenServer> getter, int reloadIntervalSeconds) {
        super(getter, reloadIntervalSeconds);
    }

    public IdGenServerDiscoveryFactory(NamedServerListGetter<IdGenServer> getter) {
        super(getter, 5);
    }

    @Override
    public IdGenServerDiscovery getDiscovery(String serviceName) {
        CamelliaDiscovery<IdGenServer> discovery = super.getDiscovery(serviceName);
        return new IdGenServerDiscovery() {
            @Override
            public List<IdGenServer> findAll() {
                return discovery.findAll();
            }

            @Override
            public void setCallback(Callback<IdGenServer> callback) {
                discovery.setCallback(callback);
            }

            @Override
            public void clearCallback(Callback<IdGenServer> callback) {
                discovery.clearCallback(callback);
            }
        };
    }
}
