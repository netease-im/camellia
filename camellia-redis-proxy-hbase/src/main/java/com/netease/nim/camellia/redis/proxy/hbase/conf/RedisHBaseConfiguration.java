package com.netease.nim.camellia.redis.proxy.hbase.conf;

import com.netease.nim.camellia.core.util.CamelliaThreadFactory;
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

    public static int zsetValueRefThreshold() {
        Integer threshold = ConfigurationUtil.getInteger(properties, "zset.valueRef.threshold", 48);
        if (threshold <= ZSET_VALUE_REF_THRESHOLD_MIN) {
            threshold = ZSET_VALUE_REF_THRESHOLD_MIN;
        }
        return threshold;
    }

    public static int zsetExpireSeconds() {
        return ConfigurationUtil.getInteger(properties, "zset.expire.seconds", 3*24*3600);
    }

    public static int zsetValueRefExpireSeconds() {
        return ConfigurationUtil.getInteger(properties, "zset.valueRef.expire.seconds", 24*3600);
    }

    public static boolean isZSetHBaseCacheNull() {
        return ConfigurationUtil.getBoolean(properties, "zset.hbase.cache.null", true);
    }

    public static byte[] nullCachePrefix() {
        String prefix = ConfigurationUtil.get(properties, "null.cache.prefix", "camellia_null");
        if (prefix == null) return null;
        return SafeEncoder.encode(prefix);
    }

    public static int nullCacheExpireSeconds() {
        return ConfigurationUtil.getInteger(properties, "null.cache.expire.seconds", 12 * 3600);
    }

    public static int notNullCacheExpireSeconds() {
        return ConfigurationUtil.getInteger(properties, "not.null.cache.expire.seconds", 60);
    }

    public static String hbaseTableName() {
        return ConfigurationUtil.get(properties, "hbase.table.name", "nim:nim_camellia_redis_hbase");
    }

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

    //expire命令是否异步执行
    public static boolean expireCommandTaskAsyncEnable() {
        return ConfigurationUtil.getBoolean(properties, "expire.command.task.async.enable", false);
    }

    //expire命令异步执行的最大批量
    public static int expireCommandTaskBatchSize() {
        return ConfigurationUtil.getInteger(properties, "expire.command.task.batch.size", 100);
    }

    public static int lockAcquireTimeoutMillis() {
        return ConfigurationUtil.getInteger(properties, "redis.acquire.timeout.millis", 2000);
    }

    public static int lockExpireMillis() {
        return ConfigurationUtil.getInteger(properties, "redis.lock.expire.millis", 2000);
    }

    public static boolean errorIfLockFail() {
        return ConfigurationUtil.getBoolean(properties, "error.if.lock.fail", true);
    }

    public static boolean isMonitorEnable() {
        return ConfigurationUtil.getBoolean(properties, "redis.hbase.monitor.enable", true);
    }

    public static int monitorIntervalSeconds() {
        return ConfigurationUtil.getInteger(properties, "redis.hbase.monitor.interval.seconds", 60);
    }
}
