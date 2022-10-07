package com.netease.nim.camellia.redis.proxy.hbase.monitor;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.redis.proxy.hbase.conf.RedisHBaseConfiguration;
import com.netease.nim.camellia.redis.proxy.util.ExecutorUtils;
import com.netease.nim.camellia.core.util.CamelliaMapUtils;
import com.netease.nim.camellia.redis.proxy.util.MaxValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;

/**
 *
 * Created by caojiajun on 2020/12/22
 */
public class RedisHBaseMonitor {

    private static final Logger logger = LoggerFactory.getLogger(RedisHBaseMonitor.class);
    private static ConcurrentHashMap<String, LongAdder> map = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, LongAdder> degradedMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Queue> queueMap = new ConcurrentHashMap<>();
    private static RedisHBaseStats redisHBaseStats = new RedisHBaseStats();


    private static final ConcurrentHashMap<String, LongAdder> thresholdExceededCount = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, LongAdder> thresholdNotExceededCount = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, LongAdder> sizeTotal = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, MaxValue> maxSize = new ConcurrentHashMap<>();

    private static ConcurrentHashMap<String, MaxValue> collectionMaxSize = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, LongAdder> collectionTotalSize = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, LongAdder> collectionSizeCount = new ConcurrentHashMap<>();

    static {
        ExecutorUtils.scheduleAtFixedRate(RedisHBaseMonitor::calc, 1, 1, TimeUnit.MINUTES);
    }

    public static void incrValueSize(String type, int size, boolean thresholdExceeded) {
        if (!RedisHBaseConfiguration.monitorEnable()) return;
        if (thresholdExceeded) {
            get(type, thresholdExceededCount, k -> new LongAdder()).increment();
        } else {
            get(type, thresholdNotExceededCount, k -> new LongAdder()).increment();
        }
        get(type, sizeTotal, k -> new LongAdder()).add(size);
        get(type, maxSize, k -> new MaxValue()).update(size);
    }

    private static <T> T get(String type, ConcurrentHashMap<String, T> map, Function<? super String, ? extends T> mappingFunction) {
        return CamelliaMapUtils.computeIfAbsent(map, type, mappingFunction);
    }

    public static void incrCollectionSize(String method, long size) {
        if (!RedisHBaseConfiguration.monitorEnable()) return;
        CamelliaMapUtils.computeIfAbsent(collectionSizeCount, method, k -> new LongAdder()).increment();
        CamelliaMapUtils.computeIfAbsent(collectionTotalSize, method, k -> new LongAdder()).add(size);
        MaxValue maxSize = CamelliaMapUtils.computeIfAbsent(collectionMaxSize, method, k -> new MaxValue());
        maxSize.update(size);
    }

    public static void incr(String method, String desc) {
        if (!RedisHBaseConfiguration.monitorEnable()) return;
        String uniqueKey = method + "|" + desc;
        LongAdder count = CamelliaMapUtils.computeIfAbsent(map, uniqueKey, k -> new LongAdder());
        count.increment();
    }

    public static void incrDegraded(String desc) {
        if (!RedisHBaseConfiguration.monitorEnable()) return;
        CamelliaMapUtils.computeIfAbsent(degradedMap, desc, k -> new LongAdder()).increment();
    }

    public static void register(String name, Queue queue) {
        queueMap.put(name, queue);
    }

    public static RedisHBaseStats getRedisHBaseStats() {
        return redisHBaseStats;
    }

    public static JSONObject getStatsJson() {
        JSONObject monitorJson = new JSONObject();

        JSONArray statsJsonArray = new JSONArray();
        for (RedisHBaseStats.Stats stats : redisHBaseStats.getStatsList()) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("method", stats.getMethod());
            jsonObject.put("desc", stats.getDesc());
            jsonObject.put("count", stats.getCount());
            statsJsonArray.add(jsonObject);
        }
        monitorJson.put("countStats", statsJsonArray);

        JSONArray statsJson2Array = new JSONArray();
        for (RedisHBaseStats.Stats2 stats2 : redisHBaseStats.getStats2List()) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("method", stats2.getMethod());
            jsonObject.put("cacheHitPercent", stats2.getCacheHitPercent());
            jsonObject.put("count", stats2.getCount());
            statsJson2Array.add(jsonObject);
        }
        monitorJson.put("cacheHitStats", statsJson2Array);

        JSONArray queueStatsJsonArray = new JSONArray();
        for (RedisHBaseStats.QueueStats queueStats : redisHBaseStats.getQueueStatsList()) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("queueName", queueStats.getQueueName());
            jsonObject.put("queueSize", queueStats.getQueueSize());
            queueStatsJsonArray.add(jsonObject);
        }
        monitorJson.put("queueStats", queueStatsJsonArray);

        JSONArray degradedStatsJsonArray = new JSONArray();
        for (RedisHBaseStats.DegradedStats degradedStats : redisHBaseStats.getDegradedStatsList()) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("desc", degradedStats.getDesc());
            jsonObject.put("count", degradedStats.getCount());
            degradedStatsJsonArray.add(jsonObject);
        }
        monitorJson.put("degradedStats", degradedStatsJsonArray);

        JSONArray zsetMemberSizeStatsJsonArray = new JSONArray();
        for (RedisHBaseStats.ValueSizeStats valueSizeStats : redisHBaseStats.getValueSizeStatsList()) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("type", valueSizeStats.getType());
            jsonObject.put("thresholdExceededCount", valueSizeStats.getThresholdExceededCount());
            jsonObject.put("thresholdNotExceededCount", valueSizeStats.getThresholdNotExceededCount());
            jsonObject.put("maxSize", valueSizeStats.getMaxSize());
            jsonObject.put("avgSize", valueSizeStats.getAvgSize());
            zsetMemberSizeStatsJsonArray.add(jsonObject);
        }
        monitorJson.put("valueSizeStats", zsetMemberSizeStatsJsonArray);

        JSONArray zsetSizeStatsJsonArray = new JSONArray();
        for (RedisHBaseStats.ZSetSizeStats zSetSizeStats : redisHBaseStats.getzSetSizeStatsList()) {
            JSONObject json = new JSONObject();
            json.put("method", zSetSizeStats.getMethod());
            json.put("avgSize", zSetSizeStats.getAvgSize());
            json.put("maxSize", zSetSizeStats.getMaxSize());
            json.put("count", zSetSizeStats.getCount());
            zsetSizeStatsJsonArray.add(json);
        }
        monitorJson.put("collectionSizeStats", zsetSizeStatsJsonArray);

        return monitorJson;
    }

    private static void calc() {
        try {
            ConcurrentHashMap<String, LongAdder> map = RedisHBaseMonitor.map;
            ConcurrentHashMap<String, LongAdder> degradedMap = RedisHBaseMonitor.degradedMap;
            RedisHBaseMonitor.map = new ConcurrentHashMap<>();
            RedisHBaseMonitor.degradedMap = new ConcurrentHashMap<>();

            Map<String, Long> cacheHitMap = new HashMap<>();
            Map<String, Long> cacheMissMap = new HashMap<>();
            List<RedisHBaseStats.Stats> statsList = new ArrayList<>();
            for (Map.Entry<String, LongAdder> entry : map.entrySet()) {
                RedisHBaseStats.Stats stats = new RedisHBaseStats.Stats();
                String[] split = entry.getKey().split("\\|");
                String method = split[0];
                String desc = split[1];
                stats.setMethod(method);
                stats.setDesc(desc);
                stats.setCount(entry.getValue().sum());
                statsList.add(stats);
                if (desc.equals(OperationType.REDIS_ONLY.name())) {
                    cacheHitMap.put(method, entry.getValue().sum());
                } else {
                    cacheMissMap.put(method, entry.getValue().sum());
                }
            }
            List<RedisHBaseStats.Stats2> stats2List = new ArrayList<>();
            Set<String> methodSet = new HashSet<>();
            methodSet.addAll(cacheHitMap.keySet());
            methodSet.addAll(cacheMissMap.keySet());
            for (String method : methodSet) {
                RedisHBaseStats.Stats2 stats2 = new RedisHBaseStats.Stats2();
                stats2.setMethod(method);
                Long cacheHit = cacheHitMap.get(method);
                cacheHit = cacheHit == null ? 0 : cacheHit;
                Long cacheMiss = cacheMissMap.get(method);
                cacheMiss = cacheMiss == null ? 0 : cacheMiss;
                long total = cacheHit + cacheMiss;
                double cacheHitPercent;
                if (total == 0) {
                    cacheHitPercent = 0.0;
                } else {
                    cacheHitPercent = cacheHit / (total * 1.0);
                }
                stats2.setCount(total);
                stats2.setCacheHitPercent(cacheHitPercent);
                stats2List.add(stats2);
            }

            List<RedisHBaseStats.QueueStats> queueStatsList = new ArrayList<>();
            for (Map.Entry<String, Queue> entry : queueMap.entrySet()) {
                RedisHBaseStats.QueueStats queueStats = new RedisHBaseStats.QueueStats();
                queueStats.setQueueName(entry.getKey());
                queueStats.setQueueSize(entry.getValue().size());
                queueStatsList.add(queueStats);
            }

            List<RedisHBaseStats.DegradedStats> degradedStatsList = new ArrayList<>();
            for (Map.Entry<String, LongAdder> entry : degradedMap.entrySet()) {
                RedisHBaseStats.DegradedStats degradedStats = new RedisHBaseStats.DegradedStats();
                degradedStats.setDesc(entry.getKey());
                degradedStats.setCount(entry.getValue().sum());
                degradedStatsList.add(degradedStats);
            }

            List<RedisHBaseStats.ValueSizeStats> valueSizeStatsList = new ArrayList<>();
            for (String type : thresholdExceededCount.keySet()) {
                RedisHBaseStats.ValueSizeStats valueSizeStats = new RedisHBaseStats.ValueSizeStats();
                valueSizeStats.setType(type);
                LongAdder longAdder1 = thresholdExceededCount.get(type);
                LongAdder longAdder2 = thresholdNotExceededCount.get(type);
                MaxValue maxValue = maxSize.get(type);
                LongAdder longAdder3 = sizeTotal.get(type);
                valueSizeStats.setThresholdExceededCount(longAdder1 == null ? 0 : longAdder1.sumThenReset());
                valueSizeStats.setThresholdNotExceededCount(longAdder2 == null ? 0 : longAdder2.sumThenReset());
                valueSizeStats.setMaxSize(maxValue == null ? 0 : maxValue.getAndSet(0));
                long sizeTotal = longAdder3 == null ? 0 : longAdder3.sumThenReset();
                long totalCount = valueSizeStats.getThresholdExceededCount() + valueSizeStats.getThresholdNotExceededCount();
                if (totalCount > 0) {
                    valueSizeStats.setAvgSize(sizeTotal * 1.0 / totalCount);
                } else {
                    valueSizeStats.setAvgSize(0.0);
                }
                valueSizeStatsList.add(valueSizeStats);
            }

            ConcurrentHashMap<String, LongAdder> zrangeTotalSize = RedisHBaseMonitor.collectionTotalSize;
            RedisHBaseMonitor.collectionTotalSize = new ConcurrentHashMap<>();
            ConcurrentHashMap<String, LongAdder> zrangeCount = RedisHBaseMonitor.collectionSizeCount;
            RedisHBaseMonitor.collectionSizeCount = new ConcurrentHashMap<>();
            ConcurrentHashMap<String, MaxValue> zrangeMaxSize = RedisHBaseMonitor.collectionMaxSize;
            RedisHBaseMonitor.collectionMaxSize = new ConcurrentHashMap<>();
            Set<String> zrangeMethodSet = new HashSet<>();
            zrangeMethodSet.addAll(zrangeTotalSize.keySet());
            zrangeMethodSet.addAll(zrangeCount.keySet());
            zrangeMethodSet.addAll(zrangeMaxSize.keySet());
            List<RedisHBaseStats.ZSetSizeStats> zSetSizeStatsList = new ArrayList<>();
            for (String method : zrangeMethodSet) {
                RedisHBaseStats.ZSetSizeStats zsetSizeStats = new RedisHBaseStats.ZSetSizeStats();
                zsetSizeStats.setMethod(method);
                LongAdder total = zrangeTotalSize.get(method);
                MaxValue max = zrangeMaxSize.get(method);
                LongAdder count = zrangeCount.get(method);
                if (count == null || total == null || count.sum() == 0) {
                    zsetSizeStats.setAvgSize(0);
                } else {
                    zsetSizeStats.setAvgSize(total.sum() * 1.0 / count.sum());
                }
                zsetSizeStats.setMaxSize(max == null ? 0 : max.get());
                zsetSizeStats.setCount(count == null ? 0 : count.sum());
                zSetSizeStatsList.add(zsetSizeStats);
            }

            RedisHBaseStats redisHBaseStats = new RedisHBaseStats();
            redisHBaseStats.setStatsList(statsList);
            redisHBaseStats.setStats2List(stats2List);
            redisHBaseStats.setQueueStatsList(queueStatsList);
            redisHBaseStats.setDegradedStatsList(degradedStatsList);
            redisHBaseStats.setValueSizeStatsList(valueSizeStatsList);
            redisHBaseStats.setzSetSizeStatsList(zSetSizeStatsList);

            RedisHBaseMonitor.redisHBaseStats = redisHBaseStats;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
