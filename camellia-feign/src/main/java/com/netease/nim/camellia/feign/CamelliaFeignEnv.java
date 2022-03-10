package com.netease.nim.camellia.feign;

import com.netease.nim.camellia.core.client.env.ProxyEnv;
import com.netease.nim.camellia.core.discovery.*;
import com.netease.nim.camellia.feign.discovery.FeignServerInfo;
import com.netease.nim.camellia.feign.resource.FeignResource;

/**
 * Created by caojiajun on 2022/3/1
 */
public class CamelliaFeignEnv {

    private CamelliaDiscoveryFactory<FeignServerInfo> discoveryFactory;
    private ProxyEnv proxyEnv = ProxyEnv.defaultProxyEnv();
    private CamelliaServerHealthChecker<FeignServerInfo> healthChecker = new DummyCamelliaServerHealthChecker<>();
    private CamelliaServerSelector<FeignResource> serverSelector = new RandomCamelliaServerSelector<>();

    private CamelliaFeignEnv() {
    }

    private CamelliaFeignEnv(ProxyEnv proxyEnv, CamelliaDiscoveryFactory<FeignServerInfo> discoveryFactory,
                             CamelliaServerHealthChecker<FeignServerInfo> healthChecker,
                             CamelliaServerSelector<FeignResource> serverSelector) {
        this.discoveryFactory = discoveryFactory;
        this.proxyEnv = proxyEnv;
        this.healthChecker = healthChecker;
        this.serverSelector = serverSelector;
    }

    public static CamelliaFeignEnv defaultFeignEnv() {
        return new CamelliaFeignEnv();
    }

    public CamelliaDiscoveryFactory<FeignServerInfo> getDiscoveryFactory() {
        return discoveryFactory;
    }

    public ProxyEnv getProxyEnv() {
        return proxyEnv;
    }

    public CamelliaServerHealthChecker<FeignServerInfo> getHealthChecker() {
        return healthChecker;
    }

    public CamelliaServerSelector<FeignResource> getServerSelector() {
        return serverSelector;
    }

    public static class Builder {
        private final CamelliaFeignEnv redisEnv;
        public Builder() {
            redisEnv = new CamelliaFeignEnv();
        }

        public Builder(CamelliaFeignEnv feignEnv) {
            this.redisEnv = new CamelliaFeignEnv(feignEnv.proxyEnv, feignEnv.discoveryFactory,
                    feignEnv.healthChecker, feignEnv.serverSelector);
        }

        public Builder healthChecker(CamelliaServerHealthChecker<FeignServerInfo> healthChecker) {
            redisEnv.healthChecker = healthChecker;
            return this;
        }

        public Builder serverSelector(CamelliaServerSelector<FeignResource> serverSelector) {
            redisEnv.serverSelector = serverSelector;
            return this;
        }

        public Builder discoveryFactory(CamelliaDiscoveryFactory<FeignServerInfo> discoveryFactory) {
            redisEnv.discoveryFactory = discoveryFactory;
            return this;
        }

        public CamelliaFeignEnv build() {
            return redisEnv;
        }
    }
}
