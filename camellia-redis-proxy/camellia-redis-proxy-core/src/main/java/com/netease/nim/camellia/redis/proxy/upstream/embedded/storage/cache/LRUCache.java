package com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.cache;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import com.netease.nim.camellia.tools.executor.CamelliaThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by caojiajun on 2025/1/8
 */
public class LRUCache<K, V> {

    private static final Logger logger = LoggerFactory.getLogger(LRUCache.class);

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1, new CamelliaThreadFactory("lru-cache-scheduler"));

    private final String name;
    private final String configKey;
    private final SizeCalculator<K> keySizeCalculator;
    private final SizeCalculator<V> valueSizeCalculator;

    private String config;

    private final ConcurrentLinkedHashMap<K, V> cache;
    private long capacity;

    private int cacheSize;

    private int loop = 0;

    public LRUCache(String name, String configKey, String defaultConfig, int estimateSizePerKV, SizeCalculator<K> keySizeCalculator, SizeCalculator<V> valueSizeCalculator) {
        this.name = name;
        this.configKey = configKey;
        this.config = defaultConfig;
        this.keySizeCalculator = keySizeCalculator;
        this.valueSizeCalculator = valueSizeCalculator;
        this.capacity = CacheCapacityConfigParser.parse(configKey, defaultConfig);
        this.config = CacheCapacityConfigParser.toString(capacity);
        this.cacheSize = (int) (capacity / estimateSizePerKV);
        this.cache = new ConcurrentLinkedHashMap.Builder<K, V>()
                .initialCapacity(cacheSize)
                .maximumWeightedCapacity(cacheSize)
                .build();
        scheduler.scheduleAtFixedRate(this::schedule, 10, 10, TimeUnit.SECONDS);
    }

    public void put(K key, V value) {
        cache.put(key, value);
    }

    public void delete(K key) {
        cache.remove(key);
    }

    public V get(K key) {
        return cache.get(key);
    }

    private void schedule() {
        this.capacity = CacheCapacityConfigParser.parse(configKey, config);
        this.config = CacheCapacityConfigParser.toString(capacity);
        long size = 0;
        long count = cache.size();
        for (Map.Entry<K, V> entry : cache.entrySet()) {
            size += keySizeCalculator.size(entry.getKey());
            size += valueSizeCalculator.size(entry.getValue());
        }
        size += count * 8;
        if (size > capacity) {
            double ratio = size * 1.0 / capacity;
            cacheSize = (int) (count / ratio);
            this.cache.setCapacity(cacheSize);
        } else {
            if (count >= cacheSize) {
                double ratio = size * 1.0 / capacity;
                cacheSize = (int) (count / ratio);
                this.cache.setCapacity(cacheSize);
            }
        }
        loop ++;
        if (loop == 6) {//print log every 60s
            logger.info("lru-cache, name = {}, target.capacity = {}, current.estimate.size = {}, current.key.count = {}, current.key.max.count = {}",
                    name, config, Utils.humanReadableByteCountBin(size), count, cacheSize);
            loop = 0;
        }
    }
}
