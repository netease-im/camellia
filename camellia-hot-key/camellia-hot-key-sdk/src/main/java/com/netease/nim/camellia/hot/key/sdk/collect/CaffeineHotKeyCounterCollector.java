package com.netease.nim.camellia.hot.key.sdk.collect;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.netease.nim.camellia.hot.key.common.model.KeyCounter;
import com.netease.nim.camellia.hot.key.common.model.KeyAction;
import com.netease.nim.camellia.tools.utils.CamelliaMapUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;

/**
 * Created by caojiajun on 2023/5/9
 */
public class CaffeineHotKeyCounterCollector implements IHotKeyCounterCollector {

    private final AtomicBoolean backUp = new AtomicBoolean(false);
    private final int capacity;
    private final ConcurrentHashMap<String, Cache<String, LongAdder>> map1;
    private final ConcurrentHashMap<String, Cache<String, LongAdder>> map2;

    public CaffeineHotKeyCounterCollector(int capacity) {
        this.capacity = capacity;
        map1 = new ConcurrentHashMap<>();
        map2 = new ConcurrentHashMap<>();
    }

    @Override
    public void push(String namespace, String key, KeyAction keyAction, long count) {
        Cache<String, LongAdder> map = getMap(namespace);
        String uniqueKey = key + "|" + keyAction.getValue();
        LongAdder adder = map.get(uniqueKey, s -> new LongAdder());
        if (adder != null) {
            adder.add(count);
        }
    }

    private Cache<String, LongAdder> getMap(String namespace) {
        ConcurrentHashMap<String, Cache<String, LongAdder>> map = !backUp.get() ? map1 : map2;
        return CamelliaMapUtils.computeIfAbsent(map, namespace, n -> Caffeine.newBuilder()
                .initialCapacity(capacity).maximumSize(capacity)
                .build());
    }

    @Override
    public synchronized List<KeyCounter> collect() {
        List<KeyCounter> result = new ArrayList<>();
        if (backUp.get()) {
            if (backUp.compareAndSet(true, false)) {
                result = toResult(map2);
                clear(map2);
            }
        } else {
            if (backUp.compareAndSet(false, true)) {
                result = toResult(map1);
                clear(map1);
            }
        }
        return result;
    }

    private void clear(ConcurrentHashMap<String, Cache<String, LongAdder>> map) {
        for (Map.Entry<String, Cache<String, LongAdder>> entry : map.entrySet()) {
            entry.getValue().invalidateAll();
        }
    }

    private List<KeyCounter> toResult(ConcurrentHashMap<String, Cache<String, LongAdder>> map) {
        List<KeyCounter> result = new ArrayList<>();
        for (Map.Entry<String, Cache<String, LongAdder>> entry : map.entrySet()) {
            String namespace = entry.getKey();
            Cache<String, LongAdder> subMap = entry.getValue();
            for (Map.Entry<String, LongAdder> subEntry : subMap.asMap().entrySet()) {
                KeyCounter counter = new KeyCounter();
                counter.setNamespace(namespace);
                String uniqueKey = subEntry.getKey();
                int index = uniqueKey.lastIndexOf("|");
                String key = uniqueKey.substring(0, index);
                KeyAction action = KeyAction.getByValue(Integer.parseInt(uniqueKey.substring(index + 1)));
                counter.setKey(key);
                counter.setAction(action);
                counter.setCount(subEntry.getValue().sum());
                result.add(counter);
            }
        }
        return result;
    }
}
