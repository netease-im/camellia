package com.netease.nim.camellia.feign;

import com.netease.nim.camellia.core.client.env.ProxyEnv;
import com.netease.nim.camellia.core.discovery.*;
import com.netease.nim.camellia.feign.discovery.FeignServerInfo;

/**
 * Created by caojiajun on 2022/3/1
 */
public class CamelliaFeignEnv {

    private ProxyEnv proxyEnv = ProxyEnv.defaultProxyEnv();
    private CamelliaDiscoveryFactory<FeignServerInfo> discoveryFactory;
    private CamelliaServerHealthChecker<FeignServerInfo> healthChecker = new DummyCamelliaServerHealthChecker<>();

    private CamelliaFeignEnv() {
    }

    private CamelliaFeignEnv(ProxyEnv proxyEnv, CamelliaDiscoveryFactory<FeignServerInfo> discoveryFactory,
                             CamelliaServerHealthChecker<FeignServerInfo> healthChecker) {
        this.discoveryFactory = discoveryFactory;
        this.proxyEnv = proxyEnv;
        this.healthChecker = healthChecker;
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

    public static class Builder {
        private final CamelliaFeignEnv redisEnv;
        public Builder() {
            redisEnv = new CamelliaFeignEnv();
        }

        public Builder(CamelliaFeignEnv feignEnv) {
            this.redisEnv = new CamelliaFeignEnv(feignEnv.proxyEnv, feignEnv.discoveryFactory, feignEnv.healthChecker);
        }

        public Builder healthChecker(CamelliaServerHealthChecker<FeignServerInfo> healthChecker) {
            redisEnv.healthChecker = healthChecker;
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
