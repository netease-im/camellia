package com.netease.nim.camellia.redis.proxy.upstream.kv.cache;

import com.netease.nim.camellia.redis.proxy.upstream.kv.conf.RedisKvConf;
import com.netease.nim.camellia.tools.sys.MemoryInfo;
import com.netease.nim.camellia.tools.sys.MemoryInfoCollector;
import io.netty.util.concurrent.DefaultThreadFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by caojiajun on 2025/2/17
 */
public class CacheCapacityCalculator {

    private static final ScheduledExecutorService scheduleService = Executors.newSingleThreadScheduledExecutor(new DefaultThreadFactory("kv-lru-cache-capacity-schedule"));

    public static void scheduleAtFixedRate(Runnable command,
                                                  long initialDelay,
                                                  long period,
                                                  TimeUnit unit) {
        scheduleService.scheduleAtFixedRate(command, initialDelay, period, unit);
    }

    public static void update(SlotLRUCache<?> cache, String namespace, String configKey) {
        long targetSize = targetSize(namespace, configKey);
        int newCapacity = (int) calcCapacity(targetSize, cache.getCapacity(), cache.size(), cache.estimateSize());
        if (newCapacity <= 0) {
            newCapacity = 10000;
        }
        cache.setCapacity(newCapacity);
    }

    private static long targetSize(String namespace, String configKey) {
        try {
            String size = RedisKvConf.getString(namespace, configKey, defaultTargetSize());
            long num = Long.parseLong(size.substring(0, size.length() - 2));
            if (size.endsWith("M")) {
                return num * 1024 * 1024L;
            } else if (size.endsWith("G")) {
                return num * 1024 * 1024 * 1024L;
            } else {
                return 1024*1024*32L;
            }
        } catch (Exception e) {
            return 1024*1024*32L;
        }
    }

    private static String defaultTargetSize() {
        MemoryInfo memoryInfo = MemoryInfoCollector.getMemoryInfo();
        long heapMemoryMax = memoryInfo.getHeapMemoryMax();
        long target = heapMemoryMax / 40 / 1024 / 1024;
        if (target <= 0) {
            target = 32;
        }
        return target + "M";
    }

    private static long calcCapacity(long targetSize, long currentCapacity, long currentCount, long currentSize) {
        if (currentCount == 0) {
            return currentCapacity;
        }
        double ratio = (double) currentSize / targetSize;
        if (ratio > 1.0) {
            return (long) (currentCount / ratio);
        } else if (ratio < 1.0) {
            ratio = ratio / (currentCount*1.0 / currentCapacity);
            return (long) (currentCapacity / ratio);
        } else {
            return currentCapacity;
        }
    }

}
