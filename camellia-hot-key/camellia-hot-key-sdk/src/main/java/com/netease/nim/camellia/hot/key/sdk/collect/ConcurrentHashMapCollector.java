package com.netease.nim.camellia.hot.key.sdk.collect;

import com.netease.nim.camellia.hot.key.common.model.KeyAction;
import com.netease.nim.camellia.hot.key.common.model.KeyCounter;
import com.netease.nim.camellia.hot.key.sdk.util.HotKeySdkUtils;
import com.netease.nim.camellia.tools.executor.CamelliaThreadFactory;
import com.netease.nim.camellia.tools.utils.CamelliaMapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;

/**
 * 超过capacity直接跳过本轮采集，而不是lru驱逐
 * Created by caojiajun on 2023/7/3
 */
public class ConcurrentHashMapCollector implements IHotKeyCounterCollector {

    private static final Logger logger = LoggerFactory.getLogger(ConcurrentHashMapCollector.class);
    private static final LongAdder failedCount = new LongAdder();
    static {
        Executors.newSingleThreadScheduledExecutor(new CamelliaThreadFactory(ConcurrentHashMapCollector.class))
                .scheduleAtFixedRate(() -> {
                    long count = failedCount.sumThenReset();
                    if (count > 0) {
                        logger.info("ConcurrentHashMapCollector full, drop count = {}", count);
                    }
                }, 30, 30, TimeUnit.SECONDS);
    }

    private final AtomicBoolean backUp = new AtomicBoolean(false);
    private final int capacity;
    private final ConcurrentHashMap<String, ConcurrentHashMapWrapper<String, LongAdderWrapper>> map1;
    private final ConcurrentHashMap<String, ConcurrentHashMapWrapper<String, LongAdderWrapper>> map2;
    private int listInitSize = HotKeySdkUtils.update(0);

    public ConcurrentHashMapCollector(int capacity) {
        this.capacity = capacity;
        map1 = new ConcurrentHashMap<>();
        map2 = new ConcurrentHashMap<>();
    }

    @Override
    public void push(String namespace, String key, KeyAction keyAction, long count) {
        ConcurrentHashMapWrapper<String, LongAdderWrapper> map = getMap(namespace);
        if (map.isFull()) {
            failedCount.add(count);
            return;
        }
        String uniqueKey = key + "|" + keyAction.getValue();
        LongAdderWrapper longAdder = CamelliaMapUtils.computeIfAbsent(map, uniqueKey, k -> new LongAdderWrapper());
        if (longAdder.isFirst()) {
            map.incrementSize();
        }
        longAdder.add(count);
    }

    private ConcurrentHashMapWrapper<String, LongAdderWrapper> getMap(String namespace) {
        ConcurrentHashMap<String, ConcurrentHashMapWrapper<String, LongAdderWrapper>> map = !backUp.get() ? map1 : map2;
        return CamelliaMapUtils.computeIfAbsent(map, namespace, n -> new ConcurrentHashMapWrapper<>(capacity));
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
        this.listInitSize = HotKeySdkUtils.update(result.size());
        return result;
    }

    private static class LongAdderWrapper extends LongAdder {

        private volatile boolean first = true;

        @Override
        public void add(long x) {
            super.add(x);
            first = false;
        }

        public boolean isFirst() {
            return first;
        }
    }

    private static class ConcurrentHashMapWrapper<K, V> extends ConcurrentHashMap<K, V> {
        private final int capacity;
        private final LongAdder size = new LongAdder();
        private volatile boolean full = false;
        public ConcurrentHashMapWrapper(int capacity) {
            this.capacity = capacity;
        }

        public void incrementSize() {
            this.size.increment();
        }

        public boolean isFull() {
            if (full) {
                return true;
            }
            full = size.sum() >= capacity;
            return full;
        }

        public void clear() {
            super.clear();
            size.reset();
            full = false;
        }
    }

    private void clear(ConcurrentHashMap<String, ConcurrentHashMapWrapper<String, LongAdderWrapper>> map) {
        for (Map.Entry<String, ConcurrentHashMapWrapper<String, LongAdderWrapper>> entry : map.entrySet()) {
            entry.getValue().clear();
        }
    }

    private List<KeyCounter> toResult(ConcurrentHashMap<String, ConcurrentHashMapWrapper<String, LongAdderWrapper>> map) {
        List<KeyCounter> result = new ArrayList<>(listInitSize);
        for (Map.Entry<String, ConcurrentHashMapWrapper<String, LongAdderWrapper>> entry : map.entrySet()) {
            String namespace = entry.getKey();
            ConcurrentHashMapWrapper<String, LongAdderWrapper> subMap = entry.getValue();
            for (Map.Entry<String, LongAdderWrapper> subEntry : subMap.entrySet()) {
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
