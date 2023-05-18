package com.netease.nim.camellia.hot.key.sdk.collect;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
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
public class HotKeyCounterCollector {

    private final AtomicBoolean backUp = new AtomicBoolean(false);
    private final int capacity;
    private final ConcurrentHashMap<String, ConcurrentLinkedHashMap<String, LongAdder>> map1;
    private final ConcurrentHashMap<String, ConcurrentLinkedHashMap<String, LongAdder>> map2;

    public HotKeyCounterCollector(int capacity) {
        this.capacity = capacity;
        map1 = new ConcurrentHashMap<>();
        map2 = new ConcurrentHashMap<>();
    }

    public void push(String namespace, String key, KeyAction keyAction, long count) {
        Map<String, LongAdder> map = getMap(namespace);
        String uniqueKey = key + "|" + keyAction.getValue();
        LongAdder adder = CamelliaMapUtils.computeIfAbsent(map, uniqueKey, k -> new LongAdder());
        adder.add(count);
    }

    private ConcurrentLinkedHashMap<String, LongAdder> getMap(String namespace) {
        ConcurrentHashMap<String, ConcurrentLinkedHashMap<String, LongAdder>> map = !backUp.get() ? map1 : map2;
        return CamelliaMapUtils.computeIfAbsent(map, namespace, n -> new ConcurrentLinkedHashMap.Builder<String, LongAdder>()
                .initialCapacity(capacity)
                .maximumWeightedCapacity(capacity)
                .build());
    }

    public synchronized List<KeyCounter> collect() {
        List<KeyCounter> result = new ArrayList<>();
        if (backUp.get()) {
            if (backUp.compareAndSet(true, false)) {
                result = toResult(map2);
                map2.clear();
            }
        } else {
            if (backUp.compareAndSet(false, true)) {
                result = toResult(map1);
                map1.clear();
            }
        }
        return result;
    }

    private List<KeyCounter> toResult(ConcurrentHashMap<String, ConcurrentLinkedHashMap<String, LongAdder>> map) {
        List<KeyCounter> result = new ArrayList<>();
        for (Map.Entry<String, ConcurrentLinkedHashMap<String, LongAdder>> entry : map.entrySet()) {
            String namespace = entry.getKey();
            ConcurrentLinkedHashMap<String, LongAdder> subMap = entry.getValue();
            for (Map.Entry<String, LongAdder> subEntry : subMap.entrySet()) {
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
