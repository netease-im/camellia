package com.netease.nim.camellia.hot.key.server.calculate;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.netease.nim.camellia.hot.key.common.model.Rule;
import com.netease.nim.camellia.hot.key.server.conf.HotKeyServerProperties;
import com.netease.nim.camellia.tools.utils.CamelliaMapUtils;
import com.netease.nim.camellia.tools.utils.MathUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by caojiajun on 2023/5/10
 */
public class HotKeyCounterManager {

    private static final Logger logger = LoggerFactory.getLogger(HotKeyCounterManager.class);

    private final ConcurrentLinkedHashMap<String, List<Cache<String, HotKeyCounter>>> counterMap;
    private final int capacity;
    private final int cacheCount;
    private final boolean is2Power;

    public HotKeyCounterManager(HotKeyServerProperties properties) {
        this.capacity = properties.getHotKeyCacheCounterCapacity();
        this.cacheCount = properties.getCacheCount();
        this.is2Power = MathUtil.is2Power(cacheCount);
        this.counterMap = new ConcurrentLinkedHashMap.Builder<String, List<Cache<String, HotKeyCounter>>>()
                .initialCapacity(properties.getMaxNamespace())
                .maximumWeightedCapacity(properties.getMaxNamespace())
                .build();
        logger.info("HotKeyCounterManager init success, maxNamespace = {}, capacity = {}, cacheCount = {}",
                properties.getMaxNamespace(), capacity, cacheCount);
    }

    public HotKeyCounter getHotKeyCounter(String namespace, String key, Rule rule) {
        List<Cache<String, HotKeyCounter>> list = getMap(namespace);
        HotKeyCounter counter;
        if (cacheCount == 1) {
            counter = list.get(0).get(key, k -> new HotKeyCounter(rule.getCheckMillis()));
        } else {
            int code = Math.abs(key.hashCode());
            int index = MathUtil.mod(is2Power, code, cacheCount);
            counter = list.get(index).get(key, k -> new HotKeyCounter(rule.getCheckMillis()));
        }
        return counter;
    }

    public void remove(String namespace) {
        counterMap.remove(namespace);
    }

    private List<Cache<String, HotKeyCounter>> getMap(String namespace) {
        return CamelliaMapUtils.computeIfAbsent(counterMap, namespace, n -> {
            List<Cache<String, HotKeyCounter>> list = new ArrayList<>(cacheCount);
            for (int i=0; i<cacheCount; i++) {
                Cache<String, HotKeyCounter> cache = Caffeine.newBuilder()
                        .initialCapacity(capacity)
                        .maximumSize(capacity)
                        .build();
                list.add(cache);
            }
            return list;
        });
    }
}
