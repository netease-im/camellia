package com.netease.nim.camellia.redis.proxy.monitor;

import com.netease.nim.camellia.redis.proxy.monitor.model.LocalLRUCacheStats;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by caojiajun on 2025/2/18
 */
public class LocalStorageLRUCacheMonitor {

    private static final ConcurrentHashMap<String, Stats> map = new ConcurrentHashMap<>();

    public static void update(String name, long capacity, long keyCount, long currentSize, long targetSize) {
        map.put(name, new Stats(capacity, keyCount, currentSize, targetSize));
    }

    public static List<LocalLRUCacheStats> collect() {
        List<LocalLRUCacheStats> statsList = new ArrayList<>();
        for (Map.Entry<String, Stats> entry : map.entrySet()) {
            LocalLRUCacheStats stats = new LocalLRUCacheStats();
            stats.setName(entry.getKey());
            stats.setCapacity(entry.getValue().capacity);
            stats.setKeyCount(entry.getValue().keyCount);
            stats.setCurrentSize(entry.getValue().currentSize);
            stats.setTargetSize(entry.getValue().targetSize);
            statsList.add(stats);
        }
        return statsList;
    }

    private static record Stats(long capacity, long keyCount, long currentSize, long targetSize) {

    }
}
