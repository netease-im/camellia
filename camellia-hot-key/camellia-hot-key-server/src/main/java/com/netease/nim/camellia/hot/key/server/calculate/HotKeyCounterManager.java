package com.netease.nim.camellia.hot.key.server.calculate;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.netease.nim.camellia.hot.key.common.model.Rule;
import com.netease.nim.camellia.hot.key.server.conf.HotKeyServerProperties;
import com.netease.nim.camellia.tools.utils.CamelliaMapUtils;

/**
 * Created by caojiajun on 2023/5/10
 */
public class HotKeyCounterManager {

    private final ConcurrentLinkedHashMap<String, ConcurrentLinkedHashMap<String, HotKeyCounter>> counterMap;
    private final int capacity;

    public HotKeyCounterManager(HotKeyServerProperties properties) {
        this.capacity = properties.getCacheCapacityPerNamespace();
        this.counterMap = new ConcurrentLinkedHashMap.Builder<String, ConcurrentLinkedHashMap<String, HotKeyCounter>>()
                .initialCapacity(properties.getMaxNamespace())
                .maximumWeightedCapacity(properties.getMaxNamespace())
                .build();
    }

    public boolean addAndCheckHot(String namespace, String key, Rule rule, long count) {
        return getHotKeyCounter(namespace, key, rule).addAndCheckHot(count);
    }

    public void remove(String namespace) {
        counterMap.remove(namespace);
    }

    private ConcurrentLinkedHashMap<String, HotKeyCounter> getMap(String namespace) {
        return CamelliaMapUtils.computeIfAbsent(counterMap, namespace, n -> new ConcurrentLinkedHashMap.Builder<String, HotKeyCounter>()
                .initialCapacity(capacity)
                .maximumWeightedCapacity(capacity)
                .build());
    }

    private HotKeyCounter getHotKeyCounter(String namespace, String key, Rule rule) {
        ConcurrentLinkedHashMap<String, HotKeyCounter> map = getMap(namespace);
        return CamelliaMapUtils.computeIfAbsent(map, key, k -> new HotKeyCounter(namespace, key, rule));
    }
}
