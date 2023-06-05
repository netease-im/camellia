package com.netease.nim.camellia.hot.key.sdk;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.hot.key.common.model.KeyAction;
import com.netease.nim.camellia.hot.key.common.model.Rule;
import com.netease.nim.camellia.hot.key.common.netty.pack.HotKeyCacheStats;
import com.netease.nim.camellia.hot.key.sdk.conf.CamelliaHotKeyCacheSdkConfig;
import com.netease.nim.camellia.hot.key.sdk.listener.CamelliaHotKeyListener;
import com.netease.nim.camellia.tools.cache.CamelliaLocalCache;
import com.netease.nim.camellia.tools.cache.NamespaceCamelliaLocalCache;
import com.netease.nim.camellia.tools.executor.CamelliaThreadFactory;
import com.netease.nim.camellia.tools.utils.CamelliaMapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;

/**
 * Created by caojiajun on 2023/5/6
 */
public class CamelliaHotKeyCacheSdk extends CamelliaHotKeyAbstractSdk implements ICamelliaHotKeyCacheSdk {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaHotKeyCacheSdk.class);

    private final CamelliaHotKeySdk sdk;
    private final CamelliaHotKeyCacheSdkConfig config;

    private final NamespaceCamelliaLocalCache hotKeyCacheKeyMap;
    private final NamespaceCamelliaLocalCache hotKeyCacheValueMap;
    private final NamespaceCamelliaLocalCache hotKeyCacheHitLockMap;

    private final NamespaceCamelliaLocalCache hotKeyCacheHitStatsMap;

    private final ConcurrentHashMap<String, AtomicBoolean> hotKeyListenerCache = new ConcurrentHashMap<>();

    public CamelliaHotKeyCacheSdk(CamelliaHotKeySdk sdk, CamelliaHotKeyCacheSdkConfig config) {
        super(sdk, config.getExecutor(), config.getScheduler(), config.getHotKeyConfigReloadIntervalSeconds());
        this.sdk = sdk;
        this.config = config;
        this.hotKeyCacheKeyMap = new NamespaceCamelliaLocalCache(config.getMaxNamespace(), config.getCapacity(), true);
        this.hotKeyCacheValueMap = new NamespaceCamelliaLocalCache(config.getMaxNamespace(), config.getCapacity(), false);
        this.hotKeyCacheHitLockMap = new NamespaceCamelliaLocalCache(config.getMaxNamespace(), config.getCapacity(), false);
        this.hotKeyCacheHitStatsMap = new NamespaceCamelliaLocalCache(config.getMaxNamespace(), config.getCapacity() * 2, false);
        logger.info("CamelliaHotKeyCacheSdk init success");
        Executors.newSingleThreadScheduledExecutor(new CamelliaThreadFactory("hot-key-cache-hit-stats-scheduler"))
                .scheduleAtFixedRate(this::reportCacheHitStats, config.getCacheHitStatsReportIntervalSeconds(), config.getCacheHitStatsReportIntervalSeconds(), TimeUnit.SECONDS);
    }

    @Override
    public void preheat(String namespace) {
        super.preheat(namespace);
        addHotKeyListener(namespace);
    }

    @Override
    public <T> T getValue(String namespace, String key, ValueLoader<T> loader) {
        try {
            //先看看是否匹配规则
            Rule rule = rulePass(namespace, key);
            if (rule == null) {
                return loader.load(key);
            }
            //提交探测请求
            sdk.push(namespace, key, KeyAction.QUERY, 1);
            addHotKeyListener(namespace);
            //看看是否是热key
            Long hotKeyExpireMillis = hotKeyCacheKeyMap.get(namespace, key, Long.class);
            if (hotKeyExpireMillis == null) {
                //如果不是热key，直接请求底层
                return loader.load(key);
            }
            //热key缓存ttl已过半，则提前穿透一次更新吧
            long ttl = hotKeyCacheKeyMap.ttl(namespace, key);
            if (ttl < hotKeyExpireMillis / 2) {
                //加个本地lock，从而只穿透一次
                boolean lock = hotKeyCacheHitLockMap.putIfAbsent(namespace, key, true, -1);
                if (lock) {
                    try {
                        return refresh(namespace, key, rule, loader);
                    } finally {
                        hotKeyCacheHitLockMap.evict(namespace, key);
                    }
                }
            }
            //如果是热key，看看有没有本地缓存
            CamelliaLocalCache.ValueWrapper valueWrapper = hotKeyCacheValueMap.get(namespace, key);
            if (valueWrapper != null) {
                cacheHit(namespace, key);
                return (T) valueWrapper.get();
            }
            return refresh(namespace, key, rule, loader);
        } catch (Exception e) {
            logger.error("getValue error, namespace = {}, key = {}", namespace, key, e);
            return loader.load(key);
        }
    }

    public <T> void setValue(String namespace, String key, T value) {
        // 检查规则
        Rule rule = rulePass(namespace, key);
        // 判断是不是热key
        if (!checkHotKey(namespace, key)) {
            return;
        }
        // 建立缓存
        refresh(namespace, key, rule, k -> value);
        // 释放穿透标记
        hotKeyCacheHitLockMap.evict(namespace, key);
    }

    public <T> T getValue(String namespace, String key) {
        try {
            //先看看是否匹配规则
            Rule rule = rulePass(namespace, key);
            if (rule == null) {
                return null;
            }
            //提交探测请求
            sdk.push(namespace, key, KeyAction.QUERY, 1);
            addHotKeyListener(namespace);
            //看看是否是热key
            Long hotKeyExpireMillis = hotKeyCacheKeyMap.get(namespace, key, Long.class);
            if (hotKeyExpireMillis == null) {
                return null;
            }
            //如果是热key，看看有没有本地缓存
            CamelliaLocalCache.ValueWrapper valueWrapper = hotKeyCacheValueMap.get(namespace, key);
            if (valueWrapper == null) {
                return null;
            }
            //热key缓存ttl已过半，则提前穿透一次更新吧
            long ttl = hotKeyCacheKeyMap.ttl(namespace, key);
            if (ttl < hotKeyExpireMillis / 2) {
                //加个本地lock，从而只穿透一次
                boolean lock = hotKeyCacheHitLockMap.putIfAbsent(namespace, key, true, -1);
                if (lock) {
                    return null;
                }
            }
            cacheHit(namespace, key);
            return (T) valueWrapper.get();
        } catch (Exception e) {
            logger.error("getValue error, namespace = {}, key = {}", namespace, key, e);
            return null;
        }
    }

    private static class Stats {
        String namespace;
        String key;
        LongAdder count;

        public Stats(String namespace, String key, LongAdder count) {
            this.namespace = namespace;
            this.key = key;
            this.count = count;
        }
    }

    private void cacheHit(String namespace, String key) {
        Stats count = hotKeyCacheHitStatsMap.get(namespace, key, Stats.class);
        if (count == null) {
            hotKeyCacheHitStatsMap.putIfAbsent(namespace, key, new Stats(namespace, key, new LongAdder()), -1);
            count = hotKeyCacheHitStatsMap.get(namespace, key, Stats.class);
        }
        if (count != null) {
            count.count.increment();
        }
    }

    private void reportCacheHitStats() {
        try {
            List<HotKeyCacheStats> statsList = new ArrayList<>();
            for (String namespace : hotKeyCacheHitStatsMap.namespaceSet()) {
                List<Object> values = hotKeyCacheHitStatsMap.values(namespace);
                for (Object value : values) {
                    if (value instanceof Stats) {
                        Object evict = hotKeyCacheHitStatsMap.evict(namespace, ((Stats) value).key);
                        if (evict instanceof Stats) {
                            Stats stats = (Stats) value;
                            HotKeyCacheStats hotKeyCacheStats = new HotKeyCacheStats();
                            hotKeyCacheStats.setNamespace(stats.namespace);
                            hotKeyCacheStats.setKey(stats.key);
                            hotKeyCacheStats.setHitCount(stats.count.sumThenReset());
                            statsList.add(hotKeyCacheStats);
                        }
                    }
                }
            }
            if (!statsList.isEmpty()) {
                sdk.sendHotkeyCacheStats(statsList);
            }
        } catch (Exception e) {
            logger.error("reportCacheHitStats error", e);
        }
    }

    @Override
    public void keyUpdate(String namespace, String key) {
        try {
            Rule rule = rulePass(namespace, key);
            if (rule == null) {
                return;
            }
            hotKeyCacheValueMap.evict(namespace, key);
            sdk.push(namespace, key, KeyAction.UPDATE, 1);
            addHotKeyListener(namespace);
        } catch (Exception e) {
            logger.error("keyUpdate error, namespace = {}, key = {}", namespace, key);
        }
    }

    @Override
    public void keyDelete(String namespace, String key) {
        try {
            Rule rule = rulePass(namespace, key);
            if (rule == null) {
                return;
            }
            hotKeyCacheValueMap.evict(namespace, key);
            sdk.push(namespace, key, KeyAction.DELETE, 1);
            addHotKeyListener(namespace);
        } catch (Exception e) {
            logger.error("keyDelete error, namespace = {}, key = {}", namespace, key);
        }
    }

    @Override
    public CamelliaHotKeyCacheSdkConfig getConfig() {
        return config;
    }

    private <T> T refresh(String namespace, String key, Rule rule, ValueLoader<T> loader) {
        //没有缓存，直接请求底层
        T value = loader.load(key);
        //判断是否缓存null
        if (!config.isCacheNull() && value == null) {
            return null;
        }
        //回填到缓存中
        hotKeyCacheValueMap.put(namespace, key, value, rule.getExpireMillis());
        return value;
    }


    private void addHotKeyListener(String namespace) {
        AtomicBoolean lock = CamelliaMapUtils.computeIfAbsent(hotKeyListenerCache, namespace, k -> new AtomicBoolean(false));
        if (lock.get()) return;
        if (lock.compareAndSet(false, true)) {
            //listener 只加一次
            sdk.addListener(namespace, (CamelliaHotKeyListener) event -> {
                try {
                    logger.info("receive HotKeyEvent = {}", JSONObject.toJSONString(event));
                    KeyAction keyAction = event.getKeyAction();
                    if (keyAction == KeyAction.QUERY) {
                        hotKeyCacheKeyMap.put(event.getNamespace(), event.getKey(), event.getExpireMillis(), event.getExpireMillis());
                    } else if (keyAction == KeyAction.DELETE || keyAction == KeyAction.UPDATE) {
                        hotKeyCacheValueMap.evict(event.getNamespace(), event.getKey());
                    }
                } catch (Exception e) {
                    logger.error("onHotKeyEvent error, event = {}", JSONObject.toJSONString(event), e);
                }
            });
        }
    }

    /**
     * Check if the key is hot-key.
     *
     * @param namespace namespace
     * @param key       key
     * @return true/false
     */
    public boolean checkHotKey(String namespace, String key) {
        //看看是否是热key
        Long hotKeyExpireMillis = hotKeyCacheKeyMap.get(namespace, key, Long.class);
        return hotKeyExpireMillis != null;
    }
}
