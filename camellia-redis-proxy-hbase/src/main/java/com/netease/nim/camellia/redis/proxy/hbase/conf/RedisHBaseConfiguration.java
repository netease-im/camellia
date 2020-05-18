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
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 *
 * Created by caojiajun on 2020/2/27.
 */
public class RedisHBaseConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(RedisHBaseConfiguration.class);

    private static Properties properties = new Properties();
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
            if (props.equals(RedisHBaseConfiguration.properties)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("{} not modify", fileName);
                }
            } else {
                RedisHBaseConfiguration.properties = props;
                logger.info("{} reload success", fileName);
            }
        } catch (Exception e) {
            logger.error("reload error", e);
        }
    }

    public static final int ZSET_VALUE_REF_THRESHOLD_MIN = 32;//阈值最小值是32
    //zset的value超过多少字节触发valueRef的二级缓存
    public static int zsetValueRefThreshold() {
        Integer threshold = ConfigurationUtil.getInteger(properties, "zset.valueRef.threshold", 48);
        if (threshold <= ZSET_VALUE_REF_THRESHOLD_MIN) {
            threshold = ZSET_VALUE_REF_THRESHOLD_MIN;
        }
        return threshold;
    }

    //zset在redis里的最大缓存时间
    public static int zsetExpireSeconds() {
        return ConfigurationUtil.getInteger(properties, "zset.expire.seconds", 3*24*3600);
    }

    //zset.valueRef的最大缓存时间
    public static int zsetValueRefExpireSeconds() {
        return ConfigurationUtil.getInteger(properties, "zset.valueRef.expire.seconds", 24*3600);
    }

    //是否缓存zset为null
    public static boolean isZSetHBaseCacheNull() {
        return ConfigurationUtil.getBoolean(properties, "zset.hbase.cache.null", true);
    }

    //null缓存的前缀
    public static byte[] nullCachePrefix() {
        String prefix = ConfigurationUtil.get(properties, "null.cache.prefix", "camellia_null");
        if (prefix == null) return null;
        return SafeEncoder.encode(prefix);
    }

    //null缓存的过期时间
    public static int nullCacheExpireSeconds() {
        return ConfigurationUtil.getInteger(properties, "null.cache.expire.seconds", 12 * 3600);
    }

    //null缓存的反向缓存的过期时间，用于并发控制，应设置较小的时间
    public static int notNullCacheExpireSeconds() {
        return ConfigurationUtil.getInteger(properties, "not.null.cache.expire.seconds", 60);
    }

    //hbase表名
    public static String hbaseTableName() {
        return ConfigurationUtil.get(properties, "hbase.table.name", "nim:nim_camellia_redis_hbase");
    }

    //redis里的key前缀
    public static byte[] redisKeyPrefix() {
        String prefix = ConfigurationUtil.get(properties, "redis.key.prefix", "camellia");
        if (prefix == null) return null;
        return SafeEncoder.encode(prefix);
    }

    //判断redis key是否存在，是否使用本地缓存
    public static boolean redisKeyCheckExistsLocalCacheEnable() {
        return ConfigurationUtil.getBoolean(properties, "redis.key.check.exists.local.cache.enable", false);
    }

    //判断redis key是否存在的本地缓存的容量
    public static int redisKeyCheckExistsLocalCacheCapacity() {
        return ConfigurationUtil.getInteger(properties, "redis.key.check.exists.local.cache.capacity", 100000);
    }

    //判断redis key是否存在的本地缓存过期时间，单位：毫秒
    public static long redisKeyCheckExistsExpireMillis() {
        return ConfigurationUtil.getLong(properties, "redis.key.check.exists.expire.millis", 100L);
    }

    //判断redis key是否存在的本地缓存，是否只缓存exists的场景
    public static boolean redisKeyCheckExistsLocalCacheOnlyExists() {
        return ConfigurationUtil.getBoolean(properties, "redis.key.check.exists.local.cache.only.exists", true);
    }

    //检查redis hbase的type，是否使用本地缓存
    public static boolean redisHBaseTypeLocalCacheEnable() {
        return ConfigurationUtil.getBoolean(properties, "redis.hbase.type.local.cache.enable", false);
    }

    //检查redis hbase的type，本地缓存容量
    public static int redisHBaseTypeLocalCacheCapacity() {
        return ConfigurationUtil.getInteger(properties, "redis.hbase.type.local.cache.capacity", 100000);
    }

    //检查redis hbase的type，本地缓存过期时间，单位：毫秒
    public static long redisHBaseTypeLocalCacheExpireMillis() {
        return ConfigurationUtil.getLong(properties, "redis.hbase.type.local.cache.expire.millis", 1000L);
    }

    //并发锁的acquire超时时间
    public static int lockAcquireTimeoutMillis() {
        return ConfigurationUtil.getInteger(properties, "redis.acquire.timeout.millis", 2000);
    }

    //并发锁的过期时间
    public static int lockExpireMillis() {
        return ConfigurationUtil.getInteger(properties, "redis.lock.expire.millis", 2000);
    }

    //并发锁获取失败是否抛异常
    public static boolean errorIfLockFail() {
        return ConfigurationUtil.getBoolean(properties, "error.if.lock.fail", true);
    }

    //是否开启监控
    public static boolean isMonitorEnable() {
        return ConfigurationUtil.getBoolean(properties, "redis.hbase.monitor.enable", true);
    }

    //监控间隔秒数
    public static int monitorIntervalSeconds() {
        return ConfigurationUtil.getInteger(properties, "redis.hbase.monitor.interval.seconds", 60);
    }

    //hbase异步写是否开关
    public static boolean hbaseWriteOpeAsyncEnable() {
        return ConfigurationUtil.getBoolean(properties, "hbase.write.ope.async.enable", false);
    }

    //hbase异步写的redis队列前缀
    public static String hbaseWriteAsyncTopicPrefix() {
        return ConfigurationUtil.get(properties, "hbase.write.async.topic.prefix", "camellia_redis_hbase_");
    }

    //hbase异步写的redis的topic数量（生产端）
    public static int hbaseWriteAsyncTopicProducerCount() {
        return ConfigurationUtil.getInteger(properties, "hbase.write.async.topic.producer.count", 1);
    }

    //hbase异步写的redis的topic数量（消费端）
    public static int hbaseWriteAsyncTopicConsumerCount() {
        int producerCount = hbaseWriteAsyncTopicProducerCount();
        Integer consumerCount = ConfigurationUtil.getInteger(properties, "hbase.write.async.topic.consumer.count", 1);
        if (producerCount > consumerCount) return producerCount;
        return consumerCount;
    }

    //hbase异步写的flush线程的检查间隔秒数（用于检查消费端flush线程的启动和负载均衡）
    public static int hbaseWriteAsyncFlushThreadCheckIntervalSeconds() {
        return ConfigurationUtil.getInteger(properties, "hbase.write.async.flush.thread.check.interval.seconds", 5);
    }

    //hbase异步写的批量大小
    public static int hbaseWriteAsyncBatchSize() {
        return ConfigurationUtil.getInteger(properties, "hbase.write.async.batch.size", 100);
    }

    //hbase异步写线程独占锁的acquire超时时间
    public static int hbaseWriteAsyncLockAcquireTimeoutMillis() {
        return ConfigurationUtil.getInteger(properties, "hbase.write.async.lock.acquire.timeout.millis", 3000);
    }

    //hbase异步写线程独占锁的过期时间
    public static int hbaseWriteAsyncLockExpireMillis() {
        return ConfigurationUtil.getInteger(properties, "hbase.write.async.lock.expire.millis", 12*1000);
    }

    //hbase异步写线程的间隔时间（当没有消息时）
    public static int hbaseWriteAsyncConsumeIntervalMillis() {
        return ConfigurationUtil.getInteger(properties, "hbase.write.async.consume.interval.millis", 100);
    }

    //expire相关命令是否异步执行
    public static boolean expireAsyncEnable() {
        return ConfigurationUtil.getBoolean(properties, "expire.async.enable", false);
    }

    //expire异步队列
    public static int expireAsyncQueueSize() {
        return ConfigurationUtil.getInteger(properties, "expire.async.queue.size", 100000);
    }

    //expire异步执行线程数
    public static int expireAsyncThreadSize() {
        return ConfigurationUtil.getInteger(properties, "expire.async.thread.size", SysUtils.getCpuNum() * 32);
    }

    //expire异步批量大小
    public static int expireAsyncBatchSize() {
        return ConfigurationUtil.getInteger(properties, "expire.async.batch.size", 100);
    }

    //延长redis注册的间隔，单位：秒
    public static int registerRenewIntervalSeconds() {
        return ConfigurationUtil.getInteger(properties, "register.renew.interval.seconds", 3);
    }

    //注册到redis时的过期时间，单位：秒
    public static int registerExpireSeconds() {
        return ConfigurationUtil.getInteger(properties, "register.expire.seconds", 8);
    }
}
