package com.netease.nim.camellia.redis.proxy.hbase.monitor;

import com.netease.nim.camellia.core.util.CamelliaThreadFactory;
import com.netease.nim.camellia.redis.proxy.hbase.conf.RedisHBaseConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * Created by caojiajun on 2020/3/5.
 */
public class RedisHBaseMonitor {

    private static final Logger logger = LoggerFactory.getLogger("redis-hbase-stats");

    private static ConcurrentHashMap<String, AtomicLong> map = new ConcurrentHashMap<>();
    private static AtomicLong zsetValueSizeTotal = new AtomicLong(0L);
    private static AtomicLong zsetValueSizeCount = new AtomicLong(0L);
    private static AtomicLong zsetValueSizeMax = new AtomicLong(0L);
    private static AtomicLong zsetValueNotHitThresholdCount = new AtomicLong(0L);
    private static AtomicLong zsetValueHitThresholdCount = new AtomicLong(0L);
    private static ExecutorService exec = Executors.newFixedThreadPool(1);

    private static RedisHBaseStats redisHBaseStats = new RedisHBaseStats();

    static {
        int seconds = RedisHBaseConfiguration.monitorIntervalSeconds();
        Executors.newSingleThreadScheduledExecutor(new CamelliaThreadFactory(RedisHBaseMonitor.class))
                .scheduleAtFixedRate(RedisHBaseMonitor::calc, seconds, seconds, TimeUnit.SECONDS);
    }

    public static void incr(String method, OpeType type) {
        if (!RedisHBaseConfiguration.isMonitorEnable()) return;
        String key = method + "|" + type.name();
        AtomicLong count = map.computeIfAbsent(key, k -> new AtomicLong());
        count.incrementAndGet();
    }

    public static void zsetValueSize(int size) {
        if (!RedisHBaseConfiguration.isMonitorEnable()) return;
        exec.submit(() -> {
            zsetValueSizeTotal.addAndGet(size);
            zsetValueSizeCount.incrementAndGet();
            if (size > zsetValueSizeMax.get()) {
                zsetValueSizeMax.set(size);
            }
        });
    }

    public static void zsetValueHitThreshold(boolean hit) {
        if (!RedisHBaseConfiguration.isMonitorEnable()) return;
        if (hit) {
            zsetValueHitThresholdCount.incrementAndGet();
        } else {
            zsetValueNotHitThresholdCount.incrementAndGet();
        }
    }

    public static RedisHBaseStats getRedisHBaseStats() {
        return redisHBaseStats;
    }

    private static void calc() {
        Map<String, AtomicLong> cacheHitCountMap = new HashMap<>();
        Map<String, AtomicLong> cacheMissCountMap = new HashMap<>();
        Set<String> methodSet = new HashSet<>();
        List<RedisHBaseStats.MethodStats> methodStatsList = new ArrayList<>();
        for (Map.Entry<String, AtomicLong> entry : map.entrySet()) {
            String key = entry.getKey();
            String[] split = key.split("\\|");
            String method = split[0];
            OpeType opeType = OpeType.valueOf(split[1]);
            long count = entry.getValue().getAndSet(0);
            RedisHBaseStats.MethodStats methodStats = new RedisHBaseStats.MethodStats();
            methodStats.setMethod(method);
            methodStats.setOpeType(opeType);
            methodStats.setCount(count);
            methodStatsList.add(methodStats);
            if (opeType == OpeType.HIT_TO_HBASE || opeType == OpeType.HIT_TO_HBASE_AND_MISS) {
                AtomicLong cacheMissCount = cacheMissCountMap.computeIfAbsent(method, k -> new AtomicLong(0L));
                cacheMissCount.addAndGet(count);
            } else {
                AtomicLong cacheHitCount = cacheHitCountMap.computeIfAbsent(method, k -> new AtomicLong(0L));
                cacheHitCount.addAndGet(count);
            }
            methodSet.add(method);
        }
        long max = zsetValueSizeMax.getAndSet(0);
        long count = zsetValueSizeCount.getAndSet(0);
        long total = zsetValueSizeTotal.getAndSet(0);
        long hit = zsetValueHitThresholdCount.getAndSet(0);
        long notHit = zsetValueNotHitThresholdCount.getAndSet(0);
        RedisHBaseStats.ZSetStats zSetStats = new RedisHBaseStats.ZSetStats();
        zSetStats.setZsetValueHitThresholdCount(hit);
        zSetStats.setZsetValueNotHitThresholdCount(notHit);
        if (count != 0) {
            zSetStats.setZsetValueSizeAvg(((double) total) / count);
        }
        zSetStats.setZsetValueSizeMax(max);

        List<RedisHBaseStats.MethodCacheHitStats> methodCacheHitStatsList = new ArrayList<>();
        for (String method : methodSet) {
            RedisHBaseStats.MethodCacheHitStats methodCacheHitStats = new RedisHBaseStats.MethodCacheHitStats();
            methodCacheHitStats.setMethod(method);
            AtomicLong cacheHitCount = cacheHitCountMap.get(method);
            AtomicLong cacheMissCount = cacheMissCountMap.get(method);
            if (cacheHitCount == null) {
                cacheHitCount = new AtomicLong(0L);
            }
            if (cacheMissCount == null) {
                cacheMissCount = new AtomicLong(0L);
            }
            long totalCount = cacheHitCount.get() + cacheMissCount.get();
            if (totalCount <= 0) continue;
            double cacheHitPercent = ((double) cacheHitCount.get()) / totalCount;
            methodCacheHitStats.setCount(totalCount);
            methodCacheHitStats.setCacheHitPercent(cacheHitPercent);
            methodCacheHitStatsList.add(methodCacheHitStats);
        }

        RedisHBaseStats redisHBaseStats = new RedisHBaseStats();
        redisHBaseStats.setMethodStatsList(methodStatsList);
        redisHBaseStats.setzSetStats(zSetStats);
        redisHBaseStats.setMethodCacheHitStatsList(methodCacheHitStatsList);

        RedisHBaseMonitor.redisHBaseStats = redisHBaseStats;

        if (RedisHBaseConfiguration.isMonitorEnable()) {
            logger.info(">>>>>>>START>>>>>>>");
            logger.info("====zset====");
            logger.info("zset.value.size.avg={}", zSetStats.getZsetValueSizeAvg());
            logger.info("zset.value.size.max={}", zSetStats.getZsetValueSizeMax());
            logger.info("zset.value.hit.threshold.count={}", zSetStats.getZsetValueHitThresholdCount());
            logger.info("zset.value.not.hit.threshold.count={}", zSetStats.getZsetValueNotHitThresholdCount());
            logger.info("====method====");
            for (RedisHBaseStats.MethodStats methodStats : redisHBaseStats.getMethodStatsList()) {
                logger.info("method={},opeType={},count={}", methodStats.getMethod(), methodStats.getOpeType(), methodStats.getCount());
            }
            logger.info("====method.cache.hit====");
            for (RedisHBaseStats.MethodCacheHitStats methodCacheHitStats : redisHBaseStats.getMethodCacheHitStatsList()) {
                logger.info("method={},count={},cacheHitPercent={}", methodCacheHitStats.getMethod(), methodCacheHitStats.getCount(), methodCacheHitStats.getCacheHitPercent());
            }
            logger.info("<<<<<<<END<<<<<<<");
        }
    }
}
