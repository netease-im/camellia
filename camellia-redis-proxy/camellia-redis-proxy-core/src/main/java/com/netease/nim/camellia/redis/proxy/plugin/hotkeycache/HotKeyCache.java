package com.netease.nim.camellia.redis.proxy.plugin.hotkeycache;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.netease.nim.camellia.tools.utils.BytesKey;
import com.netease.nim.camellia.tools.utils.CamelliaMapUtils;
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
 * 一段时间间隔内，达到某一个阈值，即为hot-key
 * When a certain threshold is reached within a period of time, it is a hot-key
 * Created by caojiajun on 2020/11/4
 */
public class HotKeyCache {

    private static final Logger logger = LoggerFactory.getLogger(HotKeyCache.class);

    private final IdentityInfo identityInfo;
    private final Object lockObj = new Object();
    private final ConcurrentLinkedHashMap<BytesKey, Object> refreshLockMap;
    private final ConcurrentLinkedHashMap<BytesKey, Long> lastRefreshTimeMap;

    /**
     * LRU cache container.
     */
    private final ConcurrentLinkedHashMap<BytesKey, HotValueWrapper> cache;
    private final LRUCounter hotKeyCounter;

    /**
     * Cache expiration time, in milliseconds
     */
    private final long cacheExpireMillis;
    /**
     * 时间间隔内的热key阈值
     * <p> Hot key threshold in time interval
     */
    private long hotKeyCheckThreshold;

    /**
     * Check if the key needs to be cached.
     */
    private final HotKeyCacheKeyChecker keyChecker;

    private final String CALLBACK_NAME;
    private final HotKeyCacheStatsCallback callback;

    /**
     * Cache {@code null} or not
     */
    private boolean cacheNull;
    private boolean enable;

    /**
     * BytesKey，value is hitCount
     */
    private ConcurrentHashMap<BytesKey, AtomicLong> statsMap = new ConcurrentHashMap<>();

    /**
     * @param identityInfo tenant identity information，bid + bgroup can represent one tenant.
     * @param hotKeyCacheConfig hot key cache config
     */
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
        // 热key的容量，一共计算多少热key
        int counterMaxCapacity = ProxyDynamicConf.getInt("hot.key.cache.counter.capacity",
                identityInfo.getBid(), identityInfo.getBgroup(), Constants.Server.hotKeyCacheCounterMaxCapacity);
        // 热key的时间间隔
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
     * @param key key
     * @return HotValue
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
            // 过期删除
            if (TimeCache.currentMillis - wrapper.timestamp > cacheExpireMillis) {
                cache.remove(bytesKey);
                return null;
            }
            HotValue value = wrapper.hotValue;
            if (value != null) {
                Long lastRefreshTime = lastRefreshTimeMap.get(bytesKey);
                // 当cache的过期时间已经达到一半值，打一个tag，穿透到redis进行本地缓存刷新
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
                // 计算命中
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
     * @param key key
     * @param value value
     */
    public void tryBuildHotKeyCache(byte[] key, byte[] value) {
        if (!enable) return;
        if (value == null && !cacheNull) {
            return;
        }
        // 是否需要缓存
        if (keyChecker != null && !keyChecker.needCache(identityInfo, key)) {
            return;
        }
        BytesKey bytesKey = new BytesKey(key);
        // 计数器判断有没有到达阈值
        Long count = this.hotKeyCounter.get(bytesKey);
        if (count == null || count < hotKeyCheckThreshold) {
            return;
        }
        // 建立缓存
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

    /**
     * 记录value的时间戳
     * Record the timestamp of the value.
     */
    private static class HotValueWrapper {
        private final long timestamp = TimeCache.currentMillis;
        private final HotValue hotValue;

        public HotValueWrapper(HotValue hotValue) {
            this.hotValue = hotValue;
        }
    }

}
