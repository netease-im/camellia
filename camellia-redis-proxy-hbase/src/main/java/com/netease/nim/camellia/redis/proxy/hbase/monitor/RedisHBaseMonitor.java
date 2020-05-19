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

    private static final Logger logger = LoggerFactory.getLogger(RedisHBaseMonitor.class);
    private static final Logger statsLogger = LoggerFactory.getLogger("redis-hbase-stats");

    private static final ConcurrentHashMap<String, AtomicLong> readMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, AtomicLong> writeMap = new ConcurrentHashMap<>();

    private static final AtomicLong zsetValueSizeTotal = new AtomicLong(0L);
    private static final AtomicLong zsetValueSizeCount = new AtomicLong(0L);
    private static final AtomicLong zsetValueSizeMax = new AtomicLong(0L);
    private static final AtomicLong zsetValueNotHitThresholdCount = new AtomicLong(0L);
    private static final AtomicLong zsetValueHitThresholdCount = new AtomicLong(0L);
    private static final ExecutorService exec = Executors.newFixedThreadPool(1);
    private static final Set<String> hbaseAsyncWriteTopics = new HashSet<>();
    private static final Map<String, Long> hbaseAsyncWriteTopicLengthMap = new HashMap<>();
    private static final ConcurrentHashMap<String, AtomicLong> hbaseAsyncWriteTopicCountMap = new ConcurrentHashMap<>();

    private static RedisHBaseStats redisHBaseStats = new RedisHBaseStats();

    static {
        int seconds = RedisHBaseConfiguration.monitorIntervalSeconds();
        Executors.newSingleThreadScheduledExecutor(new CamelliaThreadFactory(RedisHBaseMonitor.class))
                .scheduleAtFixedRate(RedisHBaseMonitor::calc, seconds, seconds, TimeUnit.SECONDS);
    }

    public static void incrRead(String method, ReadOpeType type) {
        try {
            if (!RedisHBaseConfiguration.isMonitorEnable()) return;
            String key = method + "|" + type.name();
            AtomicLong count = readMap.computeIfAbsent(key, k -> new AtomicLong());
            count.incrementAndGet();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public static void incrWrite(String method, WriteOpeType type) {
        try {
            if (!RedisHBaseConfiguration.isMonitorEnable()) return;
            String key = method + "|" + type.name();
            AtomicLong count = writeMap.computeIfAbsent(key, k -> new AtomicLong());
            count.incrementAndGet();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public static void zsetValueSize(int size) {
        try {
            if (!RedisHBaseConfiguration.isMonitorEnable()) return;
            exec.submit(() -> {
                zsetValueSizeTotal.addAndGet(size);
                zsetValueSizeCount.incrementAndGet();
                if (size > zsetValueSizeMax.get()) {
                    zsetValueSizeMax.set(size);
                }
            });
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public static void zsetValueHitThreshold(boolean hit) {
        try {
            if (!RedisHBaseConfiguration.isMonitorEnable()) return;
            if (hit) {
                zsetValueHitThresholdCount.incrementAndGet();
            } else {
                zsetValueNotHitThresholdCount.incrementAndGet();
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public static void refreshHBaseAsyncWriteTopics(Set<String> topics) {
        try {
            if (!RedisHBaseConfiguration.isMonitorEnable()) return;
            hbaseAsyncWriteTopics.clear();
            hbaseAsyncWriteTopics.addAll(topics);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public static void refreshHBaseAsyncWriteTopicLengthMap(Map<String, Long> map) {
        try {
            if (!RedisHBaseConfiguration.isMonitorEnable()) return;
            hbaseAsyncWriteTopicLengthMap.clear();
            hbaseAsyncWriteTopicLengthMap.putAll(map);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public static void incrHBaseAsyncWriteTopicCount(String topic) {
        try {
            if (!RedisHBaseConfiguration.isMonitorEnable()) return;
            AtomicLong count = hbaseAsyncWriteTopicCountMap.computeIfAbsent(topic, k -> new AtomicLong(0L));
            count.incrementAndGet();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public static RedisHBaseStats getRedisHBaseStats() {
        return redisHBaseStats;
    }

    private static void calc() {
        try {
            Map<String, AtomicLong> cacheHitCountReadMap = new HashMap<>();
            Map<String, AtomicLong> cacheMissCountReadMap = new HashMap<>();
            Set<String> methodSet = new HashSet<>();
            List<RedisHBaseStats.ReadMethodStats> readMethodStatsList = new ArrayList<>();
            for (Map.Entry<String, AtomicLong> entry : readMap.entrySet()) {
                String key = entry.getKey();
                String[] split = key.split("\\|");
                String method = split[0];
                ReadOpeType opeType = ReadOpeType.valueOf(split[1]);
                long count = entry.getValue().getAndSet(0);
                RedisHBaseStats.ReadMethodStats methodStats = new RedisHBaseStats.ReadMethodStats();
                methodStats.setMethod(method);
                methodStats.setOpeType(opeType);
                methodStats.setCount(count);
                readMethodStatsList.add(methodStats);
                if (opeType == ReadOpeType.HIT_TO_HBASE || opeType == ReadOpeType.HIT_TO_HBASE_AND_MISS) {
                    AtomicLong cacheMissCount = cacheMissCountReadMap.computeIfAbsent(method, k -> new AtomicLong(0L));
                    cacheMissCount.addAndGet(count);
                } else {
                    AtomicLong cacheHitCount = cacheHitCountReadMap.computeIfAbsent(method, k -> new AtomicLong(0L));
                    cacheHitCount.addAndGet(count);
                }
                methodSet.add(method);
            }

            List<RedisHBaseStats.WriteMethodStats> writeMethodStatsList = new ArrayList<>();
            for (Map.Entry<String, AtomicLong> entry : writeMap.entrySet()) {
                String key = entry.getKey();
                String[] split = key.split("\\|");
                String method = split[0];
                WriteOpeType opeType = WriteOpeType.valueOf(split[1]);
                long count = entry.getValue().getAndSet(0);

                RedisHBaseStats.WriteMethodStats methodStats = new RedisHBaseStats.WriteMethodStats();
                methodStats.setMethod(method);
                methodStats.setOpeType(opeType);
                methodStats.setCount(count);
                writeMethodStatsList.add(methodStats);
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

            List<RedisHBaseStats.ReadMethodCacheHitStats> methodCacheHitStatsList = new ArrayList<>();
            for (String method : methodSet) {
                RedisHBaseStats.ReadMethodCacheHitStats methodCacheHitStats = new RedisHBaseStats.ReadMethodCacheHitStats();
                methodCacheHitStats.setMethod(method);
                AtomicLong cacheHitCount = cacheHitCountReadMap.get(method);
                AtomicLong cacheMissCount = cacheMissCountReadMap.get(method);
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

            Map<String, AtomicLong> hbaseAsyncWriteTopicCountMap = new HashMap<>(RedisHBaseMonitor.hbaseAsyncWriteTopicCountMap);
            RedisHBaseMonitor.hbaseAsyncWriteTopicCountMap.clear();
            HashSet<String> topics = new HashSet<>(hbaseAsyncWriteTopics);
            List<RedisHBaseStats.TopicStats> topicStatsList = new ArrayList<>();
            for (String topic : topics) {
                RedisHBaseStats.TopicStats topicStats = new RedisHBaseStats.TopicStats();
                topicStats.setTopic(topic);
                AtomicLong topicCount = hbaseAsyncWriteTopicCountMap.get(topic);
                if (topicCount == null) {
                    topicStats.setCount(0L);
                } else {
                    topicStats.setCount(topicCount.get());
                }
                Long length = RedisHBaseMonitor.hbaseAsyncWriteTopicLengthMap.get(topic);
                if (length == null) {
                    topicStats.setLength(0L);
                } else {
                    topicStats.setLength(length);
                }
                topicStatsList.add(topicStats);
            }

            RedisHBaseStats redisHBaseStats = new RedisHBaseStats();
            redisHBaseStats.setReadMethodStatsList(readMethodStatsList);
            redisHBaseStats.setWriteMethodStatsList(writeMethodStatsList);
            redisHBaseStats.setzSetStats(zSetStats);
            redisHBaseStats.setReadMethodCacheHitStatsList(methodCacheHitStatsList);
            redisHBaseStats.setTopicStatsList(topicStatsList);

            RedisHBaseMonitor.redisHBaseStats = redisHBaseStats;

            if (RedisHBaseConfiguration.isMonitorEnable()) {
                statsLogger.info(">>>>>>>START>>>>>>>");
                statsLogger.info("====zset====");
                statsLogger.info("zset.value.size.avg={}", zSetStats.getZsetValueSizeAvg());
                statsLogger.info("zset.value.size.max={}", zSetStats.getZsetValueSizeMax());
                statsLogger.info("zset.value.hit.threshold.count={}", zSetStats.getZsetValueHitThresholdCount());
                statsLogger.info("zset.value.not.hit.threshold.count={}", zSetStats.getZsetValueNotHitThresholdCount());
                statsLogger.info("====read.method====");
                for (RedisHBaseStats.ReadMethodStats methodStats : redisHBaseStats.getReadMethodStatsList()) {
                    statsLogger.info("read.method={},opeType={},count={}", methodStats.getMethod(), methodStats.getOpeType(), methodStats.getCount());
                }
                statsLogger.info("====read.method.cache.hit====");
                for (RedisHBaseStats.ReadMethodCacheHitStats methodCacheHitStats : redisHBaseStats.getReadMethodCacheHitStatsList()) {
                    statsLogger.info("read.method={},count={},cacheHitPercent={}", methodCacheHitStats.getMethod(), methodCacheHitStats.getCount(), methodCacheHitStats.getCacheHitPercent());
                }
                statsLogger.info("====write.method====");
                for (RedisHBaseStats.WriteMethodStats methodStats : redisHBaseStats.getWriteMethodStatsList()) {
                    statsLogger.info("write.method={},opeType={},count={}", methodStats.getMethod(), methodStats.getOpeType(), methodStats.getCount());
                }
                statsLogger.info("====hbase.async.write.topics====");
                statsLogger.info("hbase.async.write.topic.count={}", redisHBaseStats.getTopicStatsList().size());
                for (RedisHBaseStats.TopicStats topicStats : redisHBaseStats.getTopicStatsList()) {
                    statsLogger.info("hbase.async.write.topic={},length={},count={}", topicStats.getTopic(), topicStats.getLength(), topicStats.getLength());
                }
                statsLogger.info("<<<<<<<END<<<<<<<");
            }
        } catch (Exception e) {
            logger.error("calc error", e);
        }
    }
}
