package com.netease.nim.camellia.feign.conf;

import com.netease.nim.camellia.core.conf.CamelliaConfig;
import com.netease.nim.camellia.core.discovery.CamelliaServerSelector;
import com.netease.nim.camellia.core.discovery.HashCamelliaServerSelector;
import com.netease.nim.camellia.core.discovery.RandomCamelliaServerSelector;
import com.netease.nim.camellia.feign.client.DynamicOption;
import com.netease.nim.camellia.feign.client.DynamicRouteConfGetter;
import com.netease.nim.camellia.feign.resource.FeignResource;
import com.netease.nim.camellia.tools.circuitbreaker.CircuitBreakerConfig;

import java.util.concurrent.TimeUnit;

/**
 * file-based
 * Created by caojiajun on 2022/11/16
 */
public class DynamicCamelliaFeignDynamicOptionGetter implements CamelliaFeignDynamicOptionGetter {

    private final CamelliaConfig localConfig;
    private final CamelliaConfig globalConfig;

    public DynamicCamelliaFeignDynamicOptionGetter(CamelliaConfig localConfig, CamelliaConfig globalConfig) {
        this.localConfig = localConfig;
        this.globalConfig = globalConfig;
    }

    @Override
    public DynamicOption getDynamicOption(long bid, String bgroup) {
        //可以动态配置的参数
        CircuitBreakerConfig circuitBreakerConfig = new CircuitBreakerConfig();
        circuitBreakerConfig.setName("bid=" + bid + ",bgroup=" + bgroup);
        circuitBreakerConfig.setStatisticSlidingWindowTime(globalConfig.getLong(bid, bgroup, "circuit.sliding.window.time", 10*1000L));
        circuitBreakerConfig.setStatisticSlidingWindowBucketSize(globalConfig.getInteger(bid, bgroup, "circuit.sliding.window.bucket.size", 10));
        circuitBreakerConfig.setEnable(() -> globalConfig.getBoolean(bid, bgroup, "circuit.enable", true));
        circuitBreakerConfig.setForceOpen(() -> globalConfig.getBoolean(bid, bgroup, "circuit.force.open.enable", false));
        circuitBreakerConfig.setFailThresholdPercentage(() -> globalConfig.getDouble(bid, bgroup, "circuit.fail.threshold.percentage", 0.5));
        circuitBreakerConfig.setRequestVolumeThreshold(() -> globalConfig.getLong(bid, bgroup, "circuit.request.volume.threshold", 20L));
        circuitBreakerConfig.setSingleTestIntervalMillis(() -> globalConfig.getLong(bid, bgroup, "circuit.single.test.interval.millis", 5000L));
        circuitBreakerConfig.setLogEnable(() -> globalConfig.getBoolean(bid, bgroup, "circuit.log.enable", true));

        String string = globalConfig.getString(bid + ".server.selector.policy", "random");
        CamelliaServerSelector<FeignResource> serverSelector;
        if (string.equalsIgnoreCase("random")) {
            serverSelector = new RandomCamelliaServerSelector<>();
        } else if (string.equalsIgnoreCase("hash")) {
            serverSelector = new HashCamelliaServerSelector<>();
        } else {
            serverSelector = new RandomCamelliaServerSelector<>();
        }

        return new DynamicOption.Builder()
                .readTimeout(() -> globalConfig.getLong(bid, bgroup, "readTimeout", 3000L), () -> TimeUnit.MILLISECONDS)
                .connectTimeout(() -> globalConfig.getLong(bid, bgroup, "connectTimeout", 3000L), () -> TimeUnit.MILLISECONDS)
                .circuitBreakerConfig(circuitBreakerConfig)
                .serverSelector(serverSelector)
                .build();
    }

    @Override
    public DynamicRouteConfGetter getDynamicRouteConfGetter(long bid) {
        //动态参数路由功能
        return routeKey -> globalConfig.getString(bid + ".route.key." + routeKey + ".bgroup", null);
    }

    @Override
    public String getDefaultBgroup(long bid) {
        //不能动态变更，启动时即决定
        String bgroup = localConfig.getString(bid + ".camellia.feign.bgroup", null);
        if (bgroup == null) {
            bgroup = localConfig.getString("camellia.feign.default.bgroup", "default");
        }
        return bgroup;
    }
}
