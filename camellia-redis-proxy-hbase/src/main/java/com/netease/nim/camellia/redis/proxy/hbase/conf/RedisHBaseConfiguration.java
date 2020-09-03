package com.netease.nim.camellia.redis.proxy.hbase.conf;

import com.netease.nim.camellia.core.util.CamelliaThreadFactory;
import com.netease.nim.camellia.core.util.SysUtils;
import com.netease.nim.camellia.redis.proxy.hbase.util.ConfigurationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.util.SafeEncoder;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 *
 * Created by caojiajun on 2020/2/27.
 */
public class RedisHBaseConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(RedisHBaseConfiguration.class);

    private static Map<String, String> conf = new HashMap<>();
    private static final String fileName = "redis-hbase.properties";

    static {
        reload();
        Executors.newSingleThreadScheduledExecutor(new CamelliaThreadFactory(RedisHBaseConfiguration.class))
                .scheduleAtFixedRate(RedisHBaseConfiguration::reload, 10, 10, TimeUnit.MINUTES);
    }

    public static void reload() {
        URL url = RedisHBaseConfiguration.class.getClassLoader().getResource(fileName);
        if (url == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("{} not exists", fileName);
            }
            return;
        }
        try {
            Properties props = new Properties();
            props.load(new FileInputStream(new File(url.getPath())));
            Map<String, String> conf = ConfigurationUtil.propertiesToMap(props);

            if (conf.equals(new HashMap<>(RedisHBaseConfiguration.conf))) {
                if (logger.isDebugEnabled()) {
                    logger.debug("{} not modify", fileName);
                }
            } else {
                RedisHBaseConfiguration.conf = conf;
                logger.info("{} reload success", fileName);
            }
        } catch (Exception e) {
            logger.error("reload error", e);
        }
    }

    public static final int ZSET_VALUE_REF_THRESHOLD_MIN = 32;//阈值最小值是32
    //zset的value超过多少字节触发valueRef的二级缓存
    public static int zsetValueRefThreshold() {
        Integer threshold = ConfigurationUtil.getInteger(conf, "zset.valueRef.threshold", 48);
        if (threshold <= ZSET_VALUE_REF_THRESHOLD_MIN) {
            threshold = ZSET_VALUE_REF_THRESHOLD_MIN;
        }
        return threshold;
    }

    //zset在redis里的最大缓存时间
    public static int zsetExpireSeconds() {
        return ConfigurationUtil.getInteger(conf, "zset.expire.seconds", 3*24*3600);
    }

    //zset.valueRef的最大缓存时间
    public static int zsetValueRefExpireSeconds() {
        return ConfigurationUtil.getInteger(conf, "zset.valueRef.expire.seconds", 24*3600);
    }

    //是否缓存hbase不存在某个key
    public static boolean isHBaseCacheNull() {
        return ConfigurationUtil.getBoolean(conf, "hbase.cache.null", true);
    }

    //null缓存的前缀
    public static byte[] nullCachePrefix() {
        String prefix = ConfigurationUtil.get(conf, "null.cache.prefix", "camellia_null");
        if (prefix == null) return null;
        return SafeEncoder.encode(prefix);
    }

    //null缓存的过期时间
    public static int nullCacheExpireSeconds() {
        return ConfigurationUtil.getInteger(conf, "null.cache.expire.seconds", 12 * 3600);
    }

    //null缓存的反向缓存的过期时间，用于并发控制，应设置较小的时间
    public static int notNullCacheExpireSeconds() {
        return ConfigurationUtil.getInteger(conf, "not.null.cache.expire.seconds", 60);
    }

    //hbase表名
    public static String hbaseTableName() {
        return ConfigurationUtil.get(conf, "hbase.table.name", "nim:nim_camellia_redis_hbase");
    }

    //redis里的key前缀
    public static byte[] redisKeyPrefix() {
        String prefix = ConfigurationUtil.get(conf, "redis.key.prefix", "camellia");
        if (prefix == null) return null;
        return SafeEncoder.encode(prefix);
    }

    //判断redis key是否存在，是否使用本地缓存
    public static boolean redisKeyCheckExistsLocalCacheEnable() {
        return ConfigurationUtil.getBoolean(conf, "redis.key.check.exists.local.cache.enable", false);
    }

    //判断redis key是否存在的本地缓存的容量
    public static int redisKeyCheckExistsLocalCacheCapacity() {
        return ConfigurationUtil.getInteger(conf, "redis.key.check.exists.local.cache.capacity", 100000);
    }

    //判断redis key是否存在的本地缓存过期时间，单位：毫秒
    public static long redisKeyCheckExistsExpireMillis() {
        return ConfigurationUtil.getLong(conf, "redis.key.check.exists.expire.millis", 100L);
    }

    //判断redis key是否存在的本地缓存，是否只缓存exists的场景
    public static boolean redisKeyCheckExistsLocalCacheOnlyExists() {
        return ConfigurationUtil.getBoolean(conf, "redis.key.check.exists.local.cache.only.exists", true);
    }

    //检查redis hbase的type，是否使用本地缓存
    public static boolean redisHBaseTypeLocalCacheEnable() {
        return ConfigurationUtil.getBoolean(conf, "redis.hbase.type.local.cache.enable", false);
    }

    //检查redis hbase的type，本地缓存容量
    public static int redisHBaseTypeLocalCacheCapacity() {
        return ConfigurationUtil.getInteger(conf, "redis.hbase.type.local.cache.capacity", 100000);
    }

    //检查redis hbase的type，本地缓存过期时间，单位：毫秒
    public static long redisHBaseTypeLocalCacheExpireMillis() {
        return ConfigurationUtil.getLong(conf, "redis.hbase.type.local.cache.expire.millis", 1000L);
    }

    //并发锁的acquire超时时间
    public static int lockAcquireTimeoutMillis() {
        return ConfigurationUtil.getInteger(conf, "redis.acquire.timeout.millis", 2000);
    }

    //并发锁的过期时间
    public static int lockExpireMillis() {
        return ConfigurationUtil.getInteger(conf, "redis.lock.expire.millis", 2000);
    }

    //并发锁获取失败是否抛异常
    public static boolean errorIfLockFail() {
        return ConfigurationUtil.getBoolean(conf, "error.if.lock.fail", true);
    }

    //是否开启监控
    public static boolean isMonitorEnable() {
        return ConfigurationUtil.getBoolean(conf, "redis.hbase.monitor.enable", true);
    }

    //监控间隔秒数
    public static int monitorIntervalSeconds() {
        return ConfigurationUtil.getInteger(conf, "redis.hbase.monitor.interval.seconds", 60);
    }

    //hbase异步写是否开关
    public static boolean hbaseWriteOpeAsyncEnable() {
        return ConfigurationUtil.getBoolean(conf, "hbase.write.ope.async.enable", false);
    }

    //hbase异步写的redis队列前缀
    public static String hbaseWriteAsyncTopicPrefix() {
        return ConfigurationUtil.get(conf, "hbase.write.async.topic.prefix", "camellia_redis_hbase_");
    }

    //hbase异步写的redis的topic数量（生产端）
    public static int hbaseWriteAsyncTopicProducerCount() {
        return ConfigurationUtil.getInteger(conf, "hbase.write.async.topic.producer.count", 1);
    }

    //hbase异步写的redis的topic数量（消费端）
    public static int hbaseWriteAsyncTopicConsumerCount() {
        int producerCount = hbaseWriteAsyncTopicProducerCount();
        Integer consumerCount = ConfigurationUtil.getInteger(conf, "hbase.write.async.topic.consumer.count", 1);
        if (producerCount > consumerCount) return producerCount;
        return consumerCount;
    }

    //hbase异步写的flush线程的检查间隔秒数（用于检查消费端flush线程的启动和负载均衡）
    public static int hbaseWriteAsyncFlushThreadCheckIntervalSeconds() {
        return ConfigurationUtil.getInteger(conf, "hbase.write.async.flush.thread.check.interval.seconds", 5);
    }

    //hbase写操作的最大批量大小
    public static int hbaseWriteBatchMaxSize() {
        return ConfigurationUtil.getInteger(conf, "hbase.write.batch.max.size", 100);
    }

    //hbase读操作的最大批量大小
    public static int hbaseReadBatchMaxSize() {
        return ConfigurationUtil.getInteger(conf, "hbase.read.batch.max.size", 100);
    }

    //hbase异步写线程独占锁的acquire超时时间
    public static int hbaseWriteAsyncLockAcquireTimeoutMillis() {
        return ConfigurationUtil.getInteger(conf, "hbase.write.async.lock.acquire.timeout.millis", 3000);
    }

    //hbase异步写线程独占锁的过期时间
    public static int hbaseWriteAsyncLockExpireMillis() {
        return ConfigurationUtil.getInteger(conf, "hbase.write.async.lock.expire.millis", 12*1000);
    }

    //hbase异步写线程的间隔时间（当没有消息时）
    public static int hbaseWriteAsyncConsumeIntervalMillis() {
        return ConfigurationUtil.getInteger(conf, "hbase.write.async.consume.interval.millis", 100);
    }

    //expire相关命令是否异步执行
    public static boolean expireAsyncEnable() {
        return ConfigurationUtil.getBoolean(conf, "expire.async.enable", false);
    }

    //expire异步队列
    public static int expireAsyncQueueSize() {
        return ConfigurationUtil.getInteger(conf, "expire.async.queue.size", 100000);
    }

    //expire异步执行线程数
    public static int expireAsyncThreadSize() {
        return ConfigurationUtil.getInteger(conf, "expire.async.thread.size", SysUtils.getCpuNum() * 32);
    }

    //expire异步批量大小
    public static int expireAsyncBatchSize() {
        return ConfigurationUtil.getInteger(conf, "expire.async.batch.size", 100);
    }

    //延长redis注册的间隔，单位：秒
    public static int registerRenewIntervalSeconds() {
        return ConfigurationUtil.getInteger(conf, "register.renew.interval.seconds", 3);
    }

    //注册到redis时的过期时间，单位：秒
    public static int registerExpireSeconds() {
        return ConfigurationUtil.getInteger(conf, "register.expire.seconds", 8);
    }

    //hbase的put/delete操作，WAL日志的异步写是否开启
    public static boolean hbaseWALAsyncEnable() {
        return ConfigurationUtil.getBoolean(conf, "hbase.wal.async.enable", false);
    }

    //redis的mget操作，单次最大批量
    public static int redisMGetMaxBatchSize() {
        return ConfigurationUtil.getInteger(conf, "redis.mget.max.batch.size", 200);
    }

    //redis的pipeline操作，单次最大批量
    public static int redisPipelineMaxBatchSize() {
        return ConfigurationUtil.getInteger(conf, "redis.pipeline.max.batch.size", 200);
    }

    //hbase的get操作频控阈值
    public static int hbaseGetFreqThreshold() {
        return ConfigurationUtil.getInteger(conf, "hbase.get.freq.threshold", 5);
    }

    //hbase的get操作频控周期
    public static long hbaseGetFreqMillis() {
        return ConfigurationUtil.getLong(conf, "hbase.get.freq.millis", 1000L);
    }

    //是否开启频控
    public static boolean freqEnable() {
        return ConfigurationUtil.getBoolean(conf, "freq.enable", true);
    }

    //hbase get单机频控周期
    public static long hbaseGetStandaloneFreqMillis() {
        return ConfigurationUtil.getLong(conf, "hbase.get.standalone.freq.millis", 1000*10L);
    }

    //hbase get单机频控阈值
    public static int hbaseGetStandaloneFreqThreshold() {
        return ConfigurationUtil.getInteger(conf, "hbase.get.standalone.freq.threshold", 1500);
    }

    //频控操作异常时是否通过
    public static boolean freqDefaultPass() {
        return ConfigurationUtil.getBoolean(conf, "freq.default.pass", true);
    }

    //zset相关的hbase读写操作是否降级为异步，可能导致数据不一致
    public static boolean zsetHBaseDegradedAsync() {
        return ConfigurationUtil.getBoolean(conf, "zset.hbase.degraded.async", false);
    }

    //zset的redis缓存重建任务的最大值
    public static int zsetRedisRebuildTaskMaxCount() {
        return ConfigurationUtil.getInteger(conf, "zset.redis.rebuild.task.max.count", 100000);
    }

    //zset的redis缓存重建延迟任务的延迟秒数
    public static int zsetRedisRebuildTaskDelaySeconds() {
        return ConfigurationUtil.getInteger(conf, "zset.redis.rebuild.task.delay.seconds", 30);
    }

    //zset的redis缓存重建任务完成后，是否打印日志
    public static boolean zsetRedisRebuildTaskLogEnable() {
        return ConfigurationUtil.getBoolean(conf, "zset.redis.rebuild.task.log.enable", true);
    }
}
