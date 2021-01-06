package com.netease.nim.camellia.redis.proxy.command.async.hotkeycache;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.netease.nim.camellia.redis.proxy.command.async.CommandContext;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.monitor.HotKeyCacheMonitor;
import com.netease.nim.camellia.redis.proxy.util.BytesKey;
import com.netease.nim.camellia.redis.proxy.util.LRUCounter;
import com.netease.nim.camellia.redis.proxy.util.ExecutorUtils;
import com.netease.nim.camellia.redis.proxy.util.TimeCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.util.SafeEncoder;

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

    private final CommandContext commandContext;
    private final Object lockObj = new Object();
    private final ConcurrentLinkedHashMap<BytesKey, Object> refreshLockMap;
    private final ConcurrentLinkedHashMap<BytesKey, Long> lastRefreshTimeMap;

    private final ConcurrentLinkedHashMap<BytesKey, HotValueWrapper> cache;
    private final LRUCounter hotKeyCounter;

    private final long cacheExpireMillis;
    private long hotKeyCheckThreshold;

    private final HotKeyCacheKeyChecker keyChecker;
    private final HotKeyCacheStatsCallback callback;

    private boolean cacheNull;
    private boolean enable;

    private ConcurrentHashMap<BytesKey, AtomicLong> statsMap = new ConcurrentHashMap<>();

    public HotKeyCache(CommandContext commandContext, CommandHotKeyCacheConfig commandHotKeyCacheConfig) {
        this.commandContext = commandContext;
        this.keyChecker = commandHotKeyCacheConfig.getHotKeyCacheKeyChecker();
        this.callback = commandHotKeyCacheConfig.getHotKeyCacheStatsCallback();
        this.cacheExpireMillis = commandHotKeyCacheConfig.getCacheExpireMillis();
        this.hotKeyCheckThreshold = commandHotKeyCacheConfig.getCounterCheckThreshold();
        this.cacheNull = commandHotKeyCacheConfig.isNeedCacheNull();
        this.enable = true;
        ProxyDynamicConf.registerCallback(this::reloadHotKeyCacheConfig);
        reloadHotKeyCacheConfig();
        this.cache = new ConcurrentLinkedHashMap.Builder<BytesKey, HotValueWrapper>()
                .initialCapacity(commandHotKeyCacheConfig.getCacheMaxCapacity())
                .maximumWeightedCapacity(commandHotKeyCacheConfig.getCacheMaxCapacity())
                .build();
        this.hotKeyCounter = new LRUCounter(commandHotKeyCacheConfig.getCounterMaxCapacity(),
                commandHotKeyCacheConfig.getCounterMaxCapacity(), commandHotKeyCacheConfig.getCounterCheckMillis());
        int refreshMapMaxCapacity = commandHotKeyCacheConfig.getCacheMaxCapacity() * 2;
        this.lastRefreshTimeMap = new ConcurrentLinkedHashMap.Builder<BytesKey, Long>()
                .initialCapacity(refreshMapMaxCapacity).maximumWeightedCapacity(refreshMapMaxCapacity).build();
        this.refreshLockMap = new ConcurrentLinkedHashMap.Builder<BytesKey, Object>()
                .initialCapacity(refreshMapMaxCapacity).maximumWeightedCapacity(refreshMapMaxCapacity).build();

        if (callback != null) {
            long callbackIntervalSeconds = commandHotKeyCacheConfig.getHotKeyCacheStatsCallbackIntervalSeconds();
            ExecutorUtils.scheduleAtFixedRate(() -> {
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
                        HotKeyCacheMonitor.hotKeyCache(commandContext, hotKeyCacheStats, commandHotKeyCacheConfig);
                        callback.callback(commandContext, hotKeyCacheStats, commandHotKeyCacheConfig);
                    }
                } catch (Exception e) {
                    logger.error("hot key cache stats callback error", e);
                }
            }, callbackIntervalSeconds, callbackIntervalSeconds, TimeUnit.SECONDS);
        }
        logger.info("HotKeyCache init success, commandContext = {}", commandContext);
    }

    public HotValue getCache(byte[] key) {
        if (!enable) return null;
        if (keyChecker != null && !keyChecker.needCache(commandContext, key)) {
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
        return null;
    }

    public void tryBuildHotKeyCache(byte[] key, byte[] value) {
        if (!enable) return;
        if (value == null && !cacheNull) {
            return;
        }
        if (keyChecker != null && !keyChecker.needCache(commandContext, key)) {
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
            logger.debug("refresh hotKey's value success, key = {}", SafeEncoder.encode(bytesKey.getKey()));
        }
    }

    private void reloadHotKeyCacheConfig() {
        Long bid = commandContext.getBid();
        String bgroup = commandContext.getBgroup();
        this.hotKeyCheckThreshold = ProxyDynamicConf.hotKeyCacheThreshold(bid, bgroup, this.hotKeyCheckThreshold);
        this.enable = ProxyDynamicConf.hotKeyCacheEnable(bid, bgroup, this.enable);
        this.cacheNull = ProxyDynamicConf.hotKeyCacheNeedCacheNull(bid, bgroup, this.cacheNull);
    }

    private static class HotValueWrapper {
        private final long timestamp = TimeCache.currentMillis;
        private final HotValue hotValue;

        public HotValueWrapper(HotValue hotValue) {
            this.hotValue = hotValue;
        }
    }

}
