package com.netease.nim.camellia.redis.proxy.upstream.local.storage.cache;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.monitor.LocalStorageLRUCacheMonitor;
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

    private final LRUCacheName name;
    private final SizeCalculator<K> keySizeCalculator;
    private final SizeCalculator<V> valueSizeCalculator;
    private final int estimateSizePerKV;
    private final ConcurrentLinkedHashMap<K, V> cache;

    private String config;

    private boolean enable;
    private long targetSize;
    private int maxKeyCount;

    private int loop = 0;

    public LRUCache(LRUCacheName name, int estimateSizePerKV,
                    SizeCalculator<K> keySizeCalculator, SizeCalculator<V> valueSizeCalculator) {
        this.name = name;
        this.estimateSizePerKV = estimateSizePerKV;
        this.keySizeCalculator = keySizeCalculator;
        this.valueSizeCalculator = valueSizeCalculator;
        this.targetSize = name.getTargetSize(-1);
        this.config = CacheCapacityConfigParser.toString(targetSize);
        this.maxKeyCount = (int) (targetSize / estimateSizePerKV);
        this.cache = new ConcurrentLinkedHashMap.Builder<K, V>()
                .initialCapacity(maxKeyCount)
                .maximumWeightedCapacity(maxKeyCount)
                .build();
        scheduler.scheduleAtFixedRate(this::schedule, 10, 10, TimeUnit.SECONDS);
        enable = ProxyDynamicConf.getBoolean(name.getConfigKey() + ".enable", true);
        logger.info("lru-cache init, name = {}, capacity = {}, key.max.count = {}, enable = {}", name, CacheCapacityConfigParser.toString(targetSize), maxKeyCount, enable);
    }

    public void put(K key, V value) {
        if (!enable) {
            return;
        }
        cache.put(key, value);
    }

    public void delete(K key) {
        if (!enable) {
            return;
        }
        cache.remove(key);
    }

    public V get(K key) {
        if (!enable) {
            return null;
        }
        return cache.get(key);
    }

    private void schedule() {
        try {
            enable = ProxyDynamicConf.getBoolean(name.getConfigKey() + ".enable", true);
            if (!enable) {
                cache.clear();
            }
            this.targetSize = name.getTargetSize(targetSize);
            this.config = CacheCapacityConfigParser.toString(targetSize);
            long estimateSize = 0;
            long keyCount = cache.size();
            long currentCapacity = cache.capacity();
            for (Map.Entry<K, V> entry : cache.entrySet()) {
                estimateSize += keySizeCalculator.size(entry.getKey());
                estimateSize += valueSizeCalculator.size(entry.getValue());
            }
            estimateSize += keyCount * 8;

            LocalStorageLRUCacheMonitor.update(name.name(), currentCapacity, keyCount, estimateSize, targetSize);

            maxKeyCount = calcCapacity(targetSize, currentCapacity, keyCount, estimateSize);
            cache.setCapacity(maxKeyCount);

            loop ++;
            if (loop == 6) {//print log every 60s
                logger.info("lru-cache, name = {}, enable = {}, target.capacity = {}, current.estimate.size = {}, current.key.count = {}, current.key.max.count = {}",
                        name, enable, config, Utils.humanReadableByteCountBin(estimateSize), keyCount, maxKeyCount);
                loop = 0;
            }
        } catch (Exception e) {
            logger.error("lru cache schedule error, name = {}", name, e);
        }
    }

    private int calcCapacity(long targetSize, long currentCapacity, long currentKeyCount, long currentSize) {
        if (currentKeyCount == 0) {
            return (int) currentCapacity;
        }
        if (currentKeyCount <= 100 && currentSize < targetSize) {
            return (int) currentCapacity;
        }
        double sizePerKey = currentSize * 1.0 / currentKeyCount;
        long targetCapacity = (long) (targetSize / sizePerKey);
        if (targetCapacity > Integer.MAX_VALUE) {
            return (int) (targetSize / estimateSizePerKV);
        }
        int maxCapacity = ProxyDynamicConf.getInt("local.storage.lru.cache.max.capacity", 100_0000);
        return Math.min(maxCapacity, (int) targetCapacity);
    }
}
