package com.netease.nim.camellia.redis.proxy.command.async.hotkeycache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.netease.nim.camellia.core.util.CamelliaThreadFactory;
import com.netease.nim.camellia.redis.proxy.util.BytesKey;
import com.netease.nim.camellia.redis.proxy.util.TimeCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.util.SafeEncoder;

import java.time.Duration;
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

    private static final ScheduledExecutorService scheduled = Executors.newSingleThreadScheduledExecutor(new CamelliaThreadFactory("hot-key-cache-stats"));

    private final Object lockObj = new Object();
    private final ConcurrentLinkedHashMap<BytesKey, Object> refreshLockMap;
    private final ConcurrentLinkedHashMap<BytesKey, Long> lastRefreshTimeMap;

    private final Cache<BytesKey, HotValue> cache;
    private final Cache<BytesKey, AtomicLong> hotKeyCounter;

    private final long cacheExpireMillis;
    private final long hotKeyCheckThreshold;

    private final HotKeyCacheKeyChecker keyChecker;
    private final HotKeyCacheStatsCallback callback;

    private ConcurrentHashMap<BytesKey, AtomicLong> statsMap = new ConcurrentHashMap<>();

    public HotKeyCache(CommandHotKeyCacheConfig commandHotKeyCacheConfig) {
        this.keyChecker = commandHotKeyCacheConfig.getHotKeyCacheKeyChecker();
        this.callback = commandHotKeyCacheConfig.getHotKeyCacheStatsCallback();
        this.cacheExpireMillis = commandHotKeyCacheConfig.getHotKeyCacheExpireMillis();
        this.hotKeyCheckThreshold = commandHotKeyCacheConfig.getHotKeyCacheCounterCheckThreshold();
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMillis(cacheExpireMillis))
                .maximumSize(commandHotKeyCacheConfig.getHotKeyCacheMaxCapacity())
                .build();
        this.hotKeyCounter = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMillis(commandHotKeyCacheConfig.getHotKeyCacheCounterCheckMillis()))
                .maximumSize(commandHotKeyCacheConfig.getHotKeyCacheCounterMaxCapacity())
                .build();
        int refreshMapMaxCapacity = (int) Math.max(commandHotKeyCacheConfig.getHotKeyCacheMaxCapacity(),
                commandHotKeyCacheConfig.getHotKeyCacheCounterMaxCapacity());
        if (refreshMapMaxCapacity < 0) {
            refreshMapMaxCapacity = 1024;
        }
        this.lastRefreshTimeMap = new ConcurrentLinkedHashMap.Builder<BytesKey, Long>()
                .initialCapacity(refreshMapMaxCapacity).maximumWeightedCapacity(refreshMapMaxCapacity).build();
        this.refreshLockMap = new ConcurrentLinkedHashMap.Builder<BytesKey, Object>()
                .initialCapacity(refreshMapMaxCapacity).maximumWeightedCapacity(refreshMapMaxCapacity).build();

        if (callback != null) {
            long callbackIntervalSeconds = commandHotKeyCacheConfig.getHotKeyCacheStatsCallbackIntervalSeconds();
            scheduled.scheduleAtFixedRate(() -> {
                try {
                    if (HotKeyCache.this.statsMap.isEmpty()) return;
                    ConcurrentHashMap<BytesKey, AtomicLong> statsMap = HotKeyCache.this.statsMap;
                    HotKeyCache.this.statsMap = new ConcurrentHashMap<>();

                    List<HotKeyCacheStats.Stats> list = new ArrayList<>();
                    for (Map.Entry<BytesKey, AtomicLong> entry : statsMap.entrySet()) {
                        HotKeyCacheStats.Stats stats = new HotKeyCacheStats.Stats();
                        stats.setKey(entry.getKey().getKey());
                        stats.setHitCount(entry.getValue().get());
                        list.add(stats);
                    }
                    if (!list.isEmpty()) {
                        HotKeyCacheStats hotKeyCacheStats = new HotKeyCacheStats();
                        hotKeyCacheStats.setStatsList(list);
                        callback.callback(hotKeyCacheStats);
                    }
                } catch (Exception e) {
                    logger.error("hot key cache stats callback error", e);
                }
            }, callbackIntervalSeconds, callbackIntervalSeconds, TimeUnit.SECONDS);
        }
    }

    public HotValue getCache(byte[] key) {
        BytesKey bytesKey = new BytesKey(key);
        if (keyChecker != null && !keyChecker.needCache(bytesKey.getKey())) {
            return null;
        }
        AtomicLong count = this.hotKeyCounter.get(bytesKey, k -> new AtomicLong());
        if (count != null) {
            count.incrementAndGet();
        }
        HotValue value = cache.getIfPresent(bytesKey);
        if (value != null) {
            Long lastRefreshTime = lastRefreshTimeMap.get(bytesKey);
            if (lastRefreshTime != null && TimeCache.currentMillis - lastRefreshTime > cacheExpireMillis / 2) {
                Object old = refreshLockMap.putIfAbsent(bytesKey, lockObj);
                if (old == null) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("try refresh hotKey's value, key = {}", SafeEncoder.encode(bytesKey.getKey()));
                    }
                    return null;
                }
            }
            if (logger.isDebugEnabled()) {
                logger.debug("getCache of hotKey = {}", SafeEncoder.encode(bytesKey.getKey()));
            }
            if (callback != null) {
                AtomicLong hit = statsMap.computeIfAbsent(bytesKey, k -> new AtomicLong());
                hit.incrementAndGet();
            }
        }
        return value;
    }

    public void tryBuildHotKeyCache(byte[] key, byte[] value) {
        BytesKey bytesKey = new BytesKey(key);
        if (keyChecker != null && !keyChecker.needCache(bytesKey.getKey())) {
            return;
        }
        AtomicLong count = this.hotKeyCounter.getIfPresent(bytesKey);
        if (count == null || count.get() < hotKeyCheckThreshold) {
            return;
        }
        cache.put(bytesKey, new HotValue(value));
        lastRefreshTimeMap.put(bytesKey, TimeCache.currentMillis);
        refreshLockMap.remove(bytesKey);
        if (logger.isDebugEnabled()) {
            logger.debug("refresh hotKey's value success, key = {}", SafeEncoder.encode(bytesKey.getKey()));
        }
    }

}
