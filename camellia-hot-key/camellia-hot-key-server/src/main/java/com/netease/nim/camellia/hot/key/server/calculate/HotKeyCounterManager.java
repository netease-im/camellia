package com.netease.nim.camellia.hot.key.server.calculate;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.netease.nim.camellia.hot.key.common.model.HotKeyConfig;
import com.netease.nim.camellia.hot.key.common.model.Rule;
import com.netease.nim.camellia.hot.key.common.utils.RuleUtils;
import com.netease.nim.camellia.hot.key.server.conf.CacheableHotKeyConfigService;
import com.netease.nim.camellia.hot.key.server.conf.HotKeyServerProperties;
import com.netease.nim.camellia.tools.utils.CamelliaMapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;


/**
 * Created by caojiajun on 2023/5/10
 */
public class HotKeyCounterManager {

    private static final Logger logger = LoggerFactory.getLogger(HotKeyCounterManager.class);

    private final ConcurrentLinkedHashMap<String, Cache<String, HotKeyCounter>> counterMap;
    private final int capacity;
    private final CacheableHotKeyConfigService hotKeyConfigService;

    public HotKeyCounterManager(HotKeyServerProperties properties, CacheableHotKeyConfigService hotKeyConfigService) {
        this.capacity = properties.getHotKeyCacheCounterCapacity();
        this.counterMap = new ConcurrentLinkedHashMap.Builder<String, Cache<String, HotKeyCounter>>()
                .initialCapacity(properties.getMaxNamespace())
                .maximumWeightedCapacity(properties.getMaxNamespace())
                .build();
        this.hotKeyConfigService = hotKeyConfigService;
        logger.info("HotKeyCounterManager init success, maxNamespace = {}, capacity = {}", properties.getMaxNamespace(), capacity);
    }

    public HotKeyCounter getHotKeyCounter(String namespace, String key, Rule rule) {
        Cache<String, HotKeyCounter> cache = getMap(namespace);
        return cache.get(key, k -> new HotKeyCounter(rule.getCheckMillis()));
    }

    public void remove(String namespace) {
        counterMap.remove(namespace);
    }

    private Cache<String, HotKeyCounter> getMap(String namespace) {
        return CamelliaMapUtils.computeIfAbsent(counterMap, namespace, n -> {
            HotKeyConfig hotKeyConfig = hotKeyConfigService.get(namespace);
            long maxCheckMillis = RuleUtils.maxCheckMillis(hotKeyConfig);
            if (maxCheckMillis <= 0) {
                maxCheckMillis = 10*60*1000L;
            }
            return Caffeine.newBuilder()
                    .initialCapacity(capacity).maximumSize(capacity)
                    .expireAfterAccess(maxCheckMillis * 2, TimeUnit.MILLISECONDS)
                    .build();
        });
    }
}
