package com.netease.nim.camellia.redis.proxy.plugin.hotkeycache;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.netease.nim.camellia.core.util.CamelliaMapUtils;
import com.netease.nim.camellia.redis.proxy.auth.IdentityInfo;
import com.netease.nim.camellia.redis.proxy.conf.Constants;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.monitor.HotKeyCacheMonitor;
import com.netease.nim.camellia.redis.proxy.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * Created by caojiajun on 2020/11/4
 */
public class HotKeyCache {

    private static final Logger logger = LoggerFactory.getLogger(HotKeyCache.class);

    private final IdentityInfo identityInfo;
    private final Object lockObj = new Object();
    private final ConcurrentLinkedHashMap<BytesKey, Object> refreshLockMap;
    private final ConcurrentLinkedHashMap<BytesKey, Long> lastRefreshTimeMap;

    private final ConcurrentLinkedHashMap<BytesKey, HotValueWrapper> cache;
    private final LRUCounter hotKeyCounter;

    private final long cacheExpireMillis;
    private long hotKeyCheckThreshold;

    private final HotKeyCacheKeyChecker keyChecker;

    private final String CALLBACK_NAME;
    private final HotKeyCacheStatsCallback callback;

    private boolean cacheNull;
    private boolean enable;

    private ConcurrentHashMap<BytesKey, AtomicLong> statsMap = new ConcurrentHashMap<>();

    public HotKeyCache(IdentityInfo identityInfo, HotKeyCacheConfig hotKeyCacheConfig) {
        this.identityInfo = identityInfo;
        this.keyChecker = hotKeyCacheConfig.getHotKeyCacheKeyChecker();
        this.callback = hotKeyCacheConfig.getHotKeyCacheStatsCallback();
        this.CALLBACK_NAME = this.callback.getClass().getName();
        this.cacheExpireMillis = ProxyDynamicConf.getLong("hot.key.cache.expire.millis",
                identityInfo.getBid(), identityInfo.getBgroup(), Constants.Server.hotKeyCacheExpireMillis);
        ProxyDynamicConf.registerCallback(this::reloadHotKeyCacheConfig);
        reloadHotKeyCacheConfig();
        int cacheMaxCapacity = ProxyDynamicConf.getInt("hot.key.cache.max.capacity",
                identityInfo.getBid(), identityInfo.getBgroup(), Constants.Server.hotKeyCacheMaxCapacity);
        this.cache = new ConcurrentLinkedHashMap.Builder<BytesKey, HotValueWrapper>()
                .initialCapacity(cacheMaxCapacity)
                .maximumWeightedCapacity(cacheMaxCapacity)
                .build();
        int counterMaxCapacity = ProxyDynamicConf.getInt("hot.key.cache.counter.capacity",
                identityInfo.getBid(), identityInfo.getBgroup(), Constants.Server.hotKeyCacheCounterMaxCapacity);
        long counterCheckMillis = ProxyDynamicConf.getLong("hot.key.cache.counter.check.millis",
                identityInfo.getBid(), identityInfo.getBgroup(), Constants.Server.hotKeyCacheCounterCheckMillis);
        this.hotKeyCounter = new LRUCounter(counterMaxCapacity, counterMaxCapacity, counterCheckMillis);
        int refreshMapMaxCapacity = cacheMaxCapacity * 2;
        this.lastRefreshTimeMap = new ConcurrentLinkedHashMap.Builder<BytesKey, Long>()
                .initialCapacity(refreshMapMaxCapacity).maximumWeightedCapacity(refreshMapMaxCapacity).build();
        this.refreshLockMap = new ConcurrentLinkedHashMap.Builder<BytesKey, Object>()
                .initialCapacity(refreshMapMaxCapacity).maximumWeightedCapacity(refreshMapMaxCapacity).build();

        long callbackIntervalSeconds = ProxyDynamicConf.getLong("hot.key.cache.stats.callback.interval.seconds",
                identityInfo.getBid(), identityInfo.getBgroup(), Constants.Server.hotKeyCacheStatsCallbackIntervalSeconds);
        ExecutorUtils.scheduleAtFixedRate(() -> {
            try {
                if (HotKeyCache.this.statsMap.isEmpty()) return;
                ConcurrentHashMap<BytesKey, AtomicLong> statsMap = HotKeyCache.this.statsMap;
                HotKeyCache.this.statsMap = new ConcurrentHashMap<>();

                List<HotKeyCacheInfo.Stats> list = new ArrayList<>();
                for (Map.Entry<BytesKey, AtomicLong> entry : statsMap.entrySet()) {
                    HotKeyCacheInfo.Stats stats = new HotKeyCacheInfo.Stats();
                    stats.setKey(entry.getKey().getKey());
                    stats.setHitCount(entry.getValue().get());
                    list.add(stats);
                }
                HotKeyCacheInfo hotKeyCacheStats = new HotKeyCacheInfo();
                hotKeyCacheStats.setStatsList(list);
                HotKeyCacheMonitor.hotKeyCache(identityInfo, hotKeyCacheStats, counterCheckMillis, hotKeyCheckThreshold);
                ExecutorUtils.submitCallbackTask(CALLBACK_NAME, () -> callback.callback(identityInfo, hotKeyCacheStats, counterCheckMillis, hotKeyCheckThreshold));
            } catch (Exception e) {
                logger.error("hot key cache stats callback error", e);
            }
        }, callbackIntervalSeconds, callbackIntervalSeconds, TimeUnit.SECONDS);
        logger.info("HotKeyCache init success, identityInfo = {}", identityInfo);
    }

    /**
     * 获取本地缓存
     */
    public HotValue getCache(byte[] key) {
        if (!enable) return null;
        if (keyChecker != null && !keyChecker.needCache(identityInfo, key)) {
            return null;
        }
        BytesKey bytesKey = new BytesKey(key);
        this.hotKeyCounter.increment(bytesKey);
        HotValueWrapper wrapper = cache.get(bytesKey);
        if (wrapper != null) {
            if (TimeCache.currentMillis - wrapper.timestamp > cacheExpireMillis) {
                cache.remove(bytesKey);
                return null;
            }
            HotValue value = wrapper.hotValue;
            if (value != null) {
                Long lastRefreshTime = lastRefreshTimeMap.get(bytesKey);
                if (lastRefreshTime != null && TimeCache.currentMillis - lastRefreshTime > cacheExpireMillis / 2) {
                    Object old = refreshLockMap.putIfAbsent(bytesKey, lockObj);
                    if (old == null) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("try refresh hotKey's value, key = {}", Utils.bytesToString(bytesKey.getKey()));
                        }
                        return null;
                    }
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("getCache of hotKey = {}", Utils.bytesToString(bytesKey.getKey()));
                }
                if (callback != null) {
                    AtomicLong hit = CamelliaMapUtils.computeIfAbsent(statsMap, bytesKey, k -> new AtomicLong());
                    hit.incrementAndGet();
                }
            }
            return value;
        }
        return null;
    }

    /**
     * 重建缓存
     */
    public void tryBuildHotKeyCache(byte[] key, byte[] value) {
        if (!enable) return;
        if (value == null && !cacheNull) {
            return;
        }
        if (keyChecker != null && !keyChecker.needCache(identityInfo, key)) {
            return;
        }
        BytesKey bytesKey = new BytesKey(key);
        Long count = this.hotKeyCounter.get(bytesKey);
        if (count == null || count < hotKeyCheckThreshold) {
            return;
        }
        cache.put(bytesKey, new HotValueWrapper(new HotValue(value)));
        lastRefreshTimeMap.put(bytesKey, TimeCache.currentMillis);
        refreshLockMap.remove(bytesKey);
        if (logger.isDebugEnabled()) {
            logger.debug("refresh hotKey's value success, key = {}", Utils.bytesToString(bytesKey.getKey()));
        }
    }

    private void reloadHotKeyCacheConfig() {
        Long bid = identityInfo.getBid();
        String bgroup = identityInfo.getBgroup();
        this.hotKeyCheckThreshold = ProxyDynamicConf.getLong("hot.key.cache.check.threshold", bid, bgroup, Constants.Server.hotKeyCacheCounterCheckThreshold);
        this.enable = ProxyDynamicConf.getBoolean("hot.key.cache.enable", bid, bgroup, true);
        this.cacheNull = ProxyDynamicConf.getBoolean("hot.key.cache.null", bid, bgroup, Constants.Server.hotKeyCacheNeedCacheNull);
    }

    private static class HotValueWrapper {
        private final long timestamp = TimeCache.currentMillis;
        private final HotValue hotValue;

        public HotValueWrapper(HotValue hotValue) {
            this.hotValue = hotValue;
        }
    }

}
