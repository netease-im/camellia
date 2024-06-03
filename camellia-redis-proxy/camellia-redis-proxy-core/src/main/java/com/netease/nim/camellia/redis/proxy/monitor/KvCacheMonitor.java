package com.netease.nim.camellia.redis.proxy.monitor;


import com.netease.nim.camellia.redis.proxy.monitor.model.KvCacheStats;
import com.netease.nim.camellia.tools.utils.CamelliaMapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * Created by caojiajun on 2024/5/21
 */
public class KvCacheMonitor {

    private static final Logger logger = LoggerFactory.getLogger(KvCacheMonitor.class);

    private static final ConcurrentHashMap<String, Counter> map = new ConcurrentHashMap<>();

    public static void writeBuffer(String namespace, String operation) {
        Counter counter = CamelliaMapUtils.computeIfAbsent(map, namespace + "|" + operation, s -> new Counter());
        counter.writeBuffer.increment();
    }

    public static void localCache(String namespace, String operation) {
        Counter counter = CamelliaMapUtils.computeIfAbsent(map, namespace + "|" + operation, s -> new Counter());
        counter.localCache.increment();
    }

    public static void redisCache(String namespace, String operation) {
        Counter counter = CamelliaMapUtils.computeIfAbsent(map, namespace + "|" + operation, s -> new Counter());
        counter.redisCache.increment();
    }

    public static void redisCache(String namespace, String operation, long add) {
        if (add <= 0) return;
        Counter counter = CamelliaMapUtils.computeIfAbsent(map, namespace + "|" + operation, s -> new Counter());
        counter.redisCache.add(add);
    }

    public static void kvStore(String namespace, String operation) {
        Counter counter = CamelliaMapUtils.computeIfAbsent(map, namespace + "|" + operation, s -> new Counter());
        counter.kvStore.increment();
    }

    public static void kvStore(String namespace, String operation, long add) {
        if (add <= 0) return;
        Counter counter = CamelliaMapUtils.computeIfAbsent(map, namespace + "|" + operation, s -> new Counter());
        counter.kvStore.add(add);
    }

    public static List<KvCacheStats> collect() {
        List<KvCacheStats> list = new ArrayList<>();
        try {
            for (Map.Entry<String, Counter> entry : map.entrySet()) {
                KvCacheStats stats = new KvCacheStats();
                String key = entry.getKey();
                int i = key.lastIndexOf("|");
                String namespace = key.substring(0, i);
                String operation = key.substring(i+1);
                stats.setNamespace(namespace);
                stats.setOperation(operation);
                Counter counter = entry.getValue();
                long writeBuffer = counter.writeBuffer.sumThenReset();
                long local = counter.localCache.sumThenReset();
                long redis = counter.redisCache.sumThenReset();
                long store = counter.kvStore.sumThenReset();
                stats.setWriteBuffer(writeBuffer);
                stats.setLocal(local);
                stats.setRedis(redis);
                stats.setStore(store);
                long total = writeBuffer + local + redis + store;
                if (total > 0) {
                    double writeBufferHit = writeBuffer * 1.0 / total;
                    double localCacheHit = local * 1.0 / total;
                    double redisCacheHit = redis * 1.0 / total;
                    stats.setWriteBufferHit(writeBufferHit);
                    stats.setLocalCacheHit(localCacheHit);
                    stats.setRedisCacheHit(redisCacheHit);
                }
                list.add(stats);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return list;
    }

    private static class Counter {
        LongAdder writeBuffer = new LongAdder();
        LongAdder localCache = new LongAdder();
        LongAdder redisCache = new LongAdder();
        LongAdder kvStore = new LongAdder();
    }

}
