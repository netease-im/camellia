package com.netease.nim.camellia.redis.proxy.upstream.kv.cache;

import com.netease.nim.camellia.redis.proxy.monitor.KvLRUCacheMonitor;
import com.netease.nim.camellia.redis.proxy.upstream.kv.conf.RedisKvConf;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import com.netease.nim.camellia.tools.sys.MemoryInfo;
import com.netease.nim.camellia.tools.sys.MemoryInfoCollector;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by caojiajun on 2025/2/17
 */
public class CacheCapacityCalculator {

    private static final Logger logger = LoggerFactory.getLogger(CacheCapacityCalculator.class);

    private static final ScheduledExecutorService scheduleService = Executors.newSingleThreadScheduledExecutor(new DefaultThreadFactory("kv-lru-cache-capacity-schedule"));

    public static void scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        scheduleService.scheduleAtFixedRate(command, initialDelay, period, unit);
    }

    public static void update(SlotLRUCache<?> cache, String namespace, String name) {
        long targetSize = targetSize(namespace, name);
        int currentCapacity = cache.getCapacity();
        long currentKeyCount = cache.size();
        long currentSize = cache.estimateSize();

        KvLRUCacheMonitor.update(namespace, name, currentCapacity, currentKeyCount, currentSize, targetSize);

        int newCapacity = calcCapacity(targetSize, currentCapacity, currentKeyCount, currentSize);
        if (newCapacity <= 0) {
            newCapacity = 10000;
        }
        logger.info("kv lru cache capacity update, targetSize = {}, currentSize = {}, currentCapacity = {}, currentKeyCount = {}, newCapacity = {}",
                Utils.humanReadableByteCountBin(targetSize), Utils.humanReadableByteCountBin(currentSize), currentCapacity, currentKeyCount, newCapacity);
        cache.setCapacity(newCapacity);
    }

    private static long targetSize(String namespace, String name) {
        try {
            String size = RedisKvConf.getString(namespace, name + ".size", defaultTargetSize());
            long num = Long.parseLong(size.substring(0, size.length() - 1));
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
        long totalMemory = memoryInfo.getHeapMemoryMax();
        long target = totalMemory / 40 / 1024 / 1024;
        if (target <= 0) {
            target = 32;
        }
        return target + "M";
    }

    private static int calcCapacity(long targetSize, long currentCapacity, long currentKeyCount, long currentSize) {
        if (currentKeyCount == 0) {
            return (int) currentCapacity;
        }
        double sizePerKey = currentSize * 1.0 / currentKeyCount;
        long targetCapacity = (long) (targetSize / sizePerKey);
        if (targetCapacity > Integer.MAX_VALUE) {
            return 10000;
        }
        return (int) targetCapacity;
    }

}
