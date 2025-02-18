package com.netease.nim.camellia.redis.proxy.monitor;

import com.netease.nim.camellia.redis.proxy.monitor.model.KvLRUCacheStats;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by caojiajun on 2025/2/18
 */
public class KvLRUCacheMonitor {

    private static final ConcurrentHashMap<Key, Stats> map = new ConcurrentHashMap<>();

    public static void update(String namespace, String name, long capacity, long keyCount, long currentSize, long targetSize) {
        map.put(new Key(namespace, name), new Stats(capacity, keyCount, currentSize, targetSize));
    }

    public static List<KvLRUCacheStats> collect() {
        List<KvLRUCacheStats> statsList = new ArrayList<>();
        for (Map.Entry<Key, Stats> entry : map.entrySet()) {
            KvLRUCacheStats stats = new KvLRUCacheStats();
            stats.setNamespace(entry.getKey().namespace);
            stats.setName(entry.getKey().name);
            stats.setCapacity(entry.getValue().capacity);
            stats.setKeyCount(entry.getValue().keyCount);
            stats.setCurrentSize(entry.getValue().currentSize);
            stats.setTargetSize(entry.getValue().targetSize);
            statsList.add(stats);
        }
        return statsList;
    }

    private static record Key(String namespace, String name) {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Key key = (Key) o;
            return Objects.equals(name, key.name) && Objects.equals(namespace, key.namespace);
        }

        @Override
        public int hashCode() {
            return Objects.hash(namespace, name);
        }
    }

    private static record Stats(long capacity, long keyCount, long currentSize, long targetSize) {

    }
}
