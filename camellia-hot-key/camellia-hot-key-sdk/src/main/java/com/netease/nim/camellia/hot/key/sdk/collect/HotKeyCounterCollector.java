package com.netease.nim.camellia.hot.key.sdk.collect;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.netease.nim.camellia.hot.key.common.model.KeyCounter;
import com.netease.nim.camellia.hot.key.common.model.KeyAction;
import com.netease.nim.camellia.tools.utils.CamelliaMapUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;

/**
 * Created by caojiajun on 2023/5/9
 */
public class HotKeyCounterCollector {

    private final AtomicBoolean backUp = new AtomicBoolean(false);
    private final ConcurrentLinkedHashMap<UniqueKey, LongAdder> map1;
    private final ConcurrentLinkedHashMap<UniqueKey, LongAdder> map2;

    public HotKeyCounterCollector(int capacity) {
        map1 = new ConcurrentLinkedHashMap.Builder<UniqueKey, LongAdder>()
                .initialCapacity(capacity)
                .maximumWeightedCapacity(capacity)
                .build();
        map2 = new ConcurrentLinkedHashMap.Builder<UniqueKey, LongAdder>()
                .initialCapacity(capacity)
                .maximumWeightedCapacity(capacity)
                .build();
    }

    public void push(String namespace, String key, KeyAction keyAction, long count) {
        UniqueKey uniqueKey = new UniqueKey(namespace, key, keyAction);
        Map<UniqueKey, LongAdder> map = !backUp.get() ? map1 : map2;
        LongAdder adder = CamelliaMapUtils.computeIfAbsent(map, uniqueKey, k -> new LongAdder());
        adder.add(count);
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

    private List<KeyCounter> toResult(Map<UniqueKey, LongAdder> map) {
        List<KeyCounter> result = new ArrayList<>();
        for (Map.Entry<UniqueKey, LongAdder> entry : map.entrySet()) {
            UniqueKey uniqueKey = entry.getKey();
            KeyCounter counter = new KeyCounter();
            counter.setNamespace(uniqueKey.getNamespace());
            counter.setKey(uniqueKey.getKey());
            counter.setAction(uniqueKey.getAction());
            counter.setCount(entry.getValue().sum());
            result.add(counter);
        }
        return result;
    }

    private static class UniqueKey {
        private final String namespace;
        private final String key;
        private final KeyAction action;

        public UniqueKey(String namespace, String key, KeyAction action) {
            this.namespace = namespace;
            this.key = key;
            this.action = action;
        }

        public String getNamespace() {
            return namespace;
        }

        public KeyAction getAction() {
            return action;
        }

        public String getKey() {
            return key;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            UniqueKey uniqueKey = (UniqueKey) o;
            return Objects.equals(namespace, uniqueKey.namespace) && action == uniqueKey.action && Objects.equals(key, uniqueKey.key);
        }

        @Override
        public int hashCode() {
            return Objects.hash(namespace, action, key);
        }
    }
}
