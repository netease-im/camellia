package com.netease.nim.camellia.tools.circuitbreaker;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.netease.nim.camellia.tools.base.DynamicConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * 一个CamelliaCircuitBreaker的Manager
 * 可以管理多个CamelliaCircuitBreaker实例，并且可以自动管理其生命周期
 * <p>
 * Created by hekaijie on 2024/3/11
 */
public class CamelliaCircuitBreakerManager {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaCircuitBreakerManager.class);

    private final Cache<String, CamelliaCircuitBreaker> breakerCache;
    private final String name;
    private DynamicConfig config; //动态配置
    private CircuitBreakerConfig circuitBreakerConfig; //固定配置

    public CamelliaCircuitBreakerManager(String name) {
        this(name, 10000, 1, TimeUnit.DAYS, new CircuitBreakerConfig());
    }

    public CamelliaCircuitBreakerManager(String name, CircuitBreakerConfig config) {
        this(name, 10000, 1, TimeUnit.DAYS, config);
    }

    public CamelliaCircuitBreakerManager(String name, int capacity, int expireTime, TimeUnit timeUnit, CircuitBreakerConfig config) {
        this.name = name;
        this.circuitBreakerConfig = config;
        this.breakerCache = Caffeine.newBuilder()
                .initialCapacity(capacity)
                .maximumSize(capacity)
                .expireAfterAccess(expireTime, timeUnit)
                .removalListener((key, value, cause) -> {
                    if (value != null) {
                        ((CamelliaCircuitBreaker) value).close();
                    }
                }).build();
        logger.info("CamelliaCircuitBreakerManager init success, name = {}, capacity = {}, expireTime = {}, timeUnit = {}",
                name, capacity, expireTime, timeUnit);
    }

    public CamelliaCircuitBreakerManager(String name, DynamicConfig config) {
        this(name, 10000, 1, TimeUnit.DAYS, config);
    }

    public CamelliaCircuitBreakerManager(String name, int capacity, int expireTime, TimeUnit timeUnit, DynamicConfig config) {
        this.name = name;
        this.config = config;
        this.breakerCache = Caffeine.newBuilder()
                .initialCapacity(capacity)
                .maximumSize(capacity)
                .expireAfterAccess(expireTime, timeUnit)
                .removalListener((key, value, cause) -> {
                    if (value != null) {
                        ((CamelliaCircuitBreaker) value).close();
                    }
                }).build();
        logger.info("CamelliaCircuitBreakerManager init success, name = {}, capacity = {}, expireTime = {}, timeUnit = {}, dynamic config = {}",
                name, capacity, expireTime, timeUnit, config.getClass().getName());
    }

    /**
     * 获取一个CircuitBreaker实例
     * @param key 唯一key
     * @param circuitBreakerConfig 配置
     * @return CircuitBreaker实例
     */
    public CamelliaCircuitBreaker getCircuitBreaker(String key, CircuitBreakerConfig circuitBreakerConfig) {
        return breakerCache.get(key, k -> new CamelliaCircuitBreaker(circuitBreakerConfig));
    }

    /**
     * 获取一个CircuitBreaker实例
     * @param key 唯一key
     * @param configKey 配置key
     * @return CircuitBreaker实例
     */
    public CamelliaCircuitBreaker getCircuitBreaker(String key, String configKey) {
        if (this.config != null) {
            return breakerCache.get(key, k -> {
                CircuitBreakerConfig breakerConfig = getCircuitBreakerConfig(configKey, this.config);
                return new CamelliaCircuitBreaker(breakerConfig);
            });
        } else if (circuitBreakerConfig != null) {
            return breakerCache.get(key, k -> {
                CircuitBreakerConfig breakerConfig = circuitBreakerConfig.duplicate();
                breakerConfig.setName(breakerConfig.getName() + "." + configKey);
                return new CamelliaCircuitBreaker(breakerConfig);
            });
        } else {
            throw new IllegalArgumentException("DynamicConfig is null");
        }
    }

    /**
     * 获取一个CircuitBreaker实例
     * @param key 唯一key
     * @return CircuitBreaker实例
     */
    public CamelliaCircuitBreaker getCircuitBreaker(String key) {
        return getCircuitBreaker(key, key);
    }

    /**
     * 删除一个CircuitBreaker实例
     * @param key 唯一key
     */
    public void delete(String key) {
        breakerCache.invalidate(key);
    }

    /**
     * 批量删除CircuitBreaker实例
     * @param keys 唯一key列表
     */
    public void batchDelete(Iterable<String> keys) {
        breakerCache.invalidateAll(keys);
    }

    /**
     * 清空CircuitBreaker实例
     */
    public void clear() {
        breakerCache.invalidateAll();
    }

    private CircuitBreakerConfig getCircuitBreakerConfig(String configKey, DynamicConfig config) {
        CircuitBreakerConfig circuitBreakerConfig = new CircuitBreakerConfig();
        String name = DynamicConfig.wrapper(config, key(configKey, "name"), this.name + "." + configKey).get();
        circuitBreakerConfig.setName(name);

        int statisticSlidingWindowBucketSize = DynamicConfig.wrapper(config, key(configKey, "statistic.sliding.window.bucket.size"), 10).get();
        circuitBreakerConfig.setStatisticSlidingWindowBucketSize(statisticSlidingWindowBucketSize);
        long statisticSlidingWindowTime = DynamicConfig.wrapper(config, key(configKey, "statistic.sliding.window.time"), 10*1000L).get();
        circuitBreakerConfig.setStatisticSlidingWindowTime(statisticSlidingWindowTime);

        circuitBreakerConfig.setEnable(DynamicConfig.wrapper(config, key(configKey, "enable"), true));
        circuitBreakerConfig.setForceOpen(DynamicConfig.wrapper(config, key(configKey, "force.open"), false));
        circuitBreakerConfig.setFailThresholdPercentage(DynamicConfig.wrapper(config, key(configKey, "fail.threshold.percentage"), 0.5));
        circuitBreakerConfig.setRequestVolumeThreshold(DynamicConfig.wrapper(config, key(configKey, "req.volume.threshold"), 20L));
        circuitBreakerConfig.setSingleTestIntervalMillis(DynamicConfig.wrapper(config, key(configKey, "single.test.interval.millis"), 5000L));
        circuitBreakerConfig.setLogEnable(DynamicConfig.wrapper(config, key(configKey, "log.enable"), true));
        return circuitBreakerConfig;
    }

    private String key(String configKey, String key) {
        return name + "." + configKey + ".camellia.circuit.breaker." + key;
    }
}
