package com.netease.nim.camellia.feign;

import com.netease.nim.camellia.core.client.env.ProxyEnv;
import com.netease.nim.camellia.core.discovery.*;
import com.netease.nim.camellia.feign.discovery.FeignServerInfo;
import com.netease.nim.camellia.tools.executor.CamelliaThreadFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * Created by caojiajun on 2022/3/1
 */
public class CamelliaFeignEnv {

    private static final ScheduledExecutorService defaultScheduledExecutor = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors(), new CamelliaThreadFactory("camellia-feign-schedule"));

    private ProxyEnv proxyEnv = ProxyEnv.defaultProxyEnv();
    private CamelliaDiscoveryFactory<FeignServerInfo> discoveryFactory;
    private CamelliaServerHealthChecker<FeignServerInfo> healthChecker = new DummyCamelliaServerHealthChecker<>();
    private FallbackExceptionChecker fallbackExceptionChecker = new FallbackExceptionChecker.Default();
    private ScheduledExecutorService scheduledExecutor = defaultScheduledExecutor;

    private CamelliaFeignEnv() {
    }

    private CamelliaFeignEnv(ProxyEnv proxyEnv, CamelliaDiscoveryFactory<FeignServerInfo> discoveryFactory,
                             CamelliaServerHealthChecker<FeignServerInfo> healthChecker,
                             FallbackExceptionChecker fallbackExceptionChecker, ScheduledExecutorService scheduledExecutor) {
        this.proxyEnv = proxyEnv;
        this.discoveryFactory = discoveryFactory;
        this.healthChecker = healthChecker;
        this.fallbackExceptionChecker = fallbackExceptionChecker;
        this.scheduledExecutor = scheduledExecutor == null ? defaultScheduledExecutor : scheduledExecutor;
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

    public FallbackExceptionChecker getFallbackExceptionChecker() {
        return fallbackExceptionChecker;
    }

    public ScheduledExecutorService getScheduledExecutor() {
        return scheduledExecutor;
    }

    public static class Builder {
        private final CamelliaFeignEnv feignEnv;
        public Builder() {
            feignEnv = new CamelliaFeignEnv();
        }

        public Builder(CamelliaFeignEnv feignEnv) {
            this.feignEnv = new CamelliaFeignEnv(feignEnv.proxyEnv, feignEnv.discoveryFactory, feignEnv.healthChecker, feignEnv.fallbackExceptionChecker, feignEnv.scheduledExecutor);
        }

        public Builder proxyEnv(ProxyEnv proxyEnv) {
            feignEnv.proxyEnv = proxyEnv;
            return this;
        }

        public Builder healthChecker(CamelliaServerHealthChecker<FeignServerInfo> healthChecker) {
            feignEnv.healthChecker = healthChecker;
            return this;
        }

        public Builder discoveryFactory(CamelliaDiscoveryFactory<FeignServerInfo> discoveryFactory) {
            feignEnv.discoveryFactory = discoveryFactory;
            return this;
        }

        public Builder fallbackExceptionChecker(FallbackExceptionChecker fallbackExceptionChecker) {
            feignEnv.fallbackExceptionChecker = fallbackExceptionChecker;
            return this;
        }

        public Builder scheduledExecutor(ScheduledExecutorService scheduledExecutor) {
            feignEnv.scheduledExecutor = scheduledExecutor;
            return this;
        }

        public CamelliaFeignEnv build() {
            return feignEnv;
        }
    }
}
