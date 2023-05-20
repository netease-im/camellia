package com.netease.nim.camellia.hot.key.server.calculate;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.netease.nim.camellia.hot.key.common.model.Rule;
import com.netease.nim.camellia.hot.key.server.conf.HotKeyServerProperties;
import com.netease.nim.camellia.tools.utils.CamelliaMapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Created by caojiajun on 2023/5/10
 */
public class HotKeyCounterManager {

    private static final Logger logger = LoggerFactory.getLogger(HotKeyCounterManager.class);

    private final ConcurrentLinkedHashMap<String, Cache<String, HotKeyCounter>> counterMap;
    private final int capacity;

    public HotKeyCounterManager(HotKeyServerProperties properties) {
        this.capacity = properties.getHotKeyCacheCounterCapacity();
        this.counterMap = new ConcurrentLinkedHashMap.Builder<String, Cache<String, HotKeyCounter>>()
                .initialCapacity(properties.getMaxNamespace())
                .maximumWeightedCapacity(properties.getMaxNamespace())
                .build();
        logger.info("HotKeyCounterManager init success, maxNamespace = {}, capacity = {}", properties.getMaxNamespace(), capacity);
    }

    public long update(String namespace, String key, Rule rule, long count) {
        long current = getHotKeyCounter(namespace, key, rule).update(count);
        if (logger.isDebugEnabled()) {
            logger.debug("check key, namespace = {}, key = {}, count = {}, rule.name = {}, current = {}", namespace, key, count, rule.getName(), current);
        }
        return current;
    }

    public void remove(String namespace) {
        counterMap.remove(namespace);
    }

    private Cache<String, HotKeyCounter> getMap(String namespace) {
        return CamelliaMapUtils.computeIfAbsent(counterMap, namespace, s -> Caffeine.newBuilder()
                .initialCapacity(capacity).maximumSize(capacity)
                .build());
    }

    private HotKeyCounter getHotKeyCounter(String namespace, String key, Rule rule) {
        Cache<String, HotKeyCounter> cache = getMap(namespace);
        return cache.get(key, k -> new HotKeyCounter(rule.getCheckMillis()));
    }
}
