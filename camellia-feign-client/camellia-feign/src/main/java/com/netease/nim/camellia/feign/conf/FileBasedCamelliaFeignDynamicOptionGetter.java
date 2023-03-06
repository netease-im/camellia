package com.netease.nim.camellia.feign.conf;

import com.netease.nim.camellia.core.conf.AbstractCamelliaConfig;
import com.netease.nim.camellia.core.conf.FileBasedCamelliaConfig;
import com.netease.nim.camellia.feign.client.DynamicOption;
import com.netease.nim.camellia.feign.client.DynamicRouteConfGetter;
import com.netease.nim.camellia.tools.circuitbreaker.CircuitBreakerConfig;

import java.util.concurrent.TimeUnit;

/**
 * file-based
 * Created by caojiajun on 2022/11/16
 */
public class FileBasedCamelliaFeignDynamicOptionGetter implements CamelliaFeignDynamicOptionGetter {

    private final AbstractCamelliaConfig config;

    public FileBasedCamelliaFeignDynamicOptionGetter(AbstractCamelliaConfig config) {
        this.config = config;
    }

    @Override
    public DynamicOption getDynamicOption(long bid, String bgroup) {
        //可以动态配置的参数
        CircuitBreakerConfig circuitBreakerConfig = new CircuitBreakerConfig();
        circuitBreakerConfig.setName("bid=" + bid + ",bgroup=" + bgroup);
        circuitBreakerConfig.setStatisticSlidingWindowTime(config.getLong(bid, bgroup, "circuit.sliding.window.time", 10*1000L));
        circuitBreakerConfig.setStatisticSlidingWindowBucketSize(config.getInteger(bid, bgroup, "circuit.sliding.window.bucket.size", 10));
        circuitBreakerConfig.setEnable(() -> config.getBoolean(bid, bgroup, "circuit.enable", true));
        circuitBreakerConfig.setForceOpen(() -> config.getBoolean(bid, bgroup, "circuit.force.open.enable", false));
        circuitBreakerConfig.setFailThresholdPercentage(() -> config.getDouble(bid, bgroup, "circuit.fail.threshold.percentage", 0.5));
        circuitBreakerConfig.setRequestVolumeThreshold(() -> config.getLong(bid, bgroup, "circuit.request.volume.threshold", 20L));
        circuitBreakerConfig.setSingleTestIntervalMillis(() -> config.getLong(bid, bgroup, "circuit.single.test.interval.millis", 5000L));
        circuitBreakerConfig.setLogEnable(() -> config.getBoolean(bid, bgroup, "circuit.log.enable", true));
        return new DynamicOption.Builder()
                .readTimeout(() -> config.getLong(bid, bgroup, "readTimeout", 3000L), () -> TimeUnit.MILLISECONDS)
                .connectTimeout(() -> config.getLong(bid, bgroup, "connectTimeout", 3000L), () -> TimeUnit.MILLISECONDS)
                .circuitBreakerConfig(circuitBreakerConfig)
                .build();
    }

    @Override
    public DynamicRouteConfGetter getDynamicRouteConfGetter(long bid) {
        //动态参数路由功能
        return routeKey -> config.getString(bid + ".route.key." + routeKey + ".bgroup", null);
    }

    @Override
    public String getDefaultBgroup(long bid) {
        //不能动态变更，启动时即决定
        String bgroup = config.getString(bid + ".camellia.feign.bgroup", null);
        if (bgroup == null) {
            bgroup = config.getString("camellia.feign.default.bgroup", "default");
        }
        return bgroup;
    }
}
