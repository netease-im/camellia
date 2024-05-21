package com.netease.nim.camellia.redis.proxy.monitor;


import com.netease.nim.camellia.redis.proxy.monitor.model.KVCacheStats;
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
public class KVCacheMonitor {

    private static final Logger logger = LoggerFactory.getLogger(KVCacheMonitor.class);

    private static final ConcurrentHashMap<String, Counter> map = new ConcurrentHashMap<>();

    public static void localCache(String namespace, String operation) {
        Counter counter = CamelliaMapUtils.computeIfAbsent(map, namespace + "|" + operation, s -> new Counter());
        counter.localCache.increment();
    }

    public static void redisCache(String namespace, String operation) {
        Counter counter = CamelliaMapUtils.computeIfAbsent(map, namespace + "|" + operation, s -> new Counter());
        counter.redisCache.increment();
    }

    public static void kvStore(String namespace, String operation) {
        Counter counter = CamelliaMapUtils.computeIfAbsent(map, namespace + "|" + operation, s -> new Counter());
        counter.kvStore.increment();
    }

    public static List<KVCacheStats> collect() {
        List<KVCacheStats> list = new ArrayList<>();
        try {
            for (Map.Entry<String, Counter> entry : map.entrySet()) {
                KVCacheStats stats = new KVCacheStats();
                String key = entry.getKey();
                int i = key.lastIndexOf("|");
                String namespace = key.substring(0, i);
                String operation = key.substring(i+1);
                stats.setNamespace(namespace);
                stats.setOperation(operation);
                Counter counter = entry.getValue();
                long local = counter.localCache.sumThenReset();
                long redis = counter.redisCache.sumThenReset();
                long store = counter.kvStore.sumThenReset();
                stats.setLocal(local);
                stats.setRedis(redis);
                stats.setStore(store);
                if (local > 0 || redis > 0 || store > 0) {
                    double localCacheHit = local * 1.0 / (local + redis + store);
                    double redisCacheHit = redis * 1.0 / (local + redis + store);
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
        LongAdder localCache = new LongAdder();
        LongAdder redisCache = new LongAdder();
        LongAdder kvStore = new LongAdder();
    }

}
