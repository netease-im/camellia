package com.netease.nim.camellia.redis.proxy.hbase.util;

import com.netease.nim.camellia.tools.utils.MD5Util;
import com.netease.nim.camellia.hbase.CamelliaHBaseTemplate;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import com.netease.nim.camellia.redis.proxy.hbase.HBaseAsyncWriteExecutor;
import com.netease.nim.camellia.redis.proxy.hbase.conf.RedisHBaseConfiguration;
import com.netease.nim.camellia.redis.proxy.hbase.monitor.RedisHBaseMonitor;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.util.SafeEncoder;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * Created by caojiajun on 2020/12/21
 */
public class RedisHBaseUtils {

    private static final Logger logger = LoggerFactory.getLogger(RedisHBaseUtils.class);

    public static final byte[] CF_D = Bytes.toBytes("d");
    public static final byte[] COL_DATA = Bytes.toBytes("data");
    public static final byte[] COL_EXPIRE = Bytes.toBytes("expire");

    public static byte[] redisKey(byte[] key) {
        return Bytes.add(Utils.stringToBytes(RedisHBaseConfiguration.redisKeyPrefix()), key);
    }

    public static byte[] nullCacheKey(byte[] key) {
        return Bytes.add(redisKey(key), SafeEncoder.encode("~empty"));
    }

    public static byte[] hbaseRowKey(byte[] key) {
        byte[] md5 = MD5Util.md5(key);
        byte[] prefix = new byte[8];
        System.arraycopy(md5, 0, prefix,0, 8);
        return Bytes.add(prefix, key);
    }

    public static <T> List<List<T>> split(List<T> list, int maxSplit) {
        if (list == null) return null;
        List<List<T>> ret = new ArrayList<>();
        if (list.size() <= maxSplit) {
            ret.add(list);
            return ret;
        }
        List<T> subList = new ArrayList<>();
        for (T t : list) {
            subList.add(t);
            if (subList.size() >= maxSplit) {
                ret.add(subList);
                subList = new ArrayList<>();
            }
        }
        if (!subList.isEmpty()) {
            ret.add(subList);
        }
        return ret;
    }

    //32字节
    public static byte[] buildRefKey(byte[] key, byte[] value) {
        byte[] part1 = MD5Util.md5(key);
        byte[] part2 = MD5Util.md5(value);
        return Bytes.add(part1, part2);
    }

    public static String hbaseTableName() {
        return RedisHBaseConfiguration.hbaseTableName();
    }

    public static boolean isRefKey(byte[] key, byte[] value) {
        if (value.length != 32) return false;
        byte[] part1 = MD5Util.md5(key);
        if (value.length < part1.length) return false;
        for (int i=0; i<part1.length; i++) {
            if (part1[i] != value[i]) {
                return false;
            }
        }
        return true;
    }

    public static byte[] parseOriginalValue(Result result) {
        if (result == null) return null;
        return result.getValue(CF_D, COL_DATA);
    }

    public static HBaseValue parseOriginalValueCheckExpire(Result result) {
        if (result == null) return null;
        byte[] data = result.getValue(CF_D, COL_DATA);
        if (data == null) return null;
        byte[] expireTime = result.getValue(CF_D, COL_EXPIRE);
        if (expireTime == null) {
            return new HBaseValue(data, null,false);
        }
        long expire = Bytes.toLong(expireTime);
        if (expire < 0) {
            return new HBaseValue(data, null, false);
        }
        if (System.currentTimeMillis() > expire) {
            return new HBaseValue(data, 0L, true);
        }
        return new HBaseValue(data, expire - System.currentTimeMillis(), false);
    }

    public static final int ZSET_MEMBER_REF_KEY_THRESHOLD_MIN = 32;//阈值最小值是32
    public static int zsetMemberRefKeyThreshold() {
        int threshold = RedisHBaseConfiguration.zsetMemberRefKeyThreshold();
        return Math.max(threshold, ZSET_MEMBER_REF_KEY_THRESHOLD_MIN);
    }

    public static int zsetMemberRefKeyExpireSeconds() {
        return RedisHBaseConfiguration.zsetMemberRefKeyExpireSeconds();
    }

    public static int stringValueThreshold() {
        return RedisHBaseConfiguration.stringValueThreshold();
    }

    public static long stringValueExpireMillis(Long expireMillis) {
        if (expireMillis == null) return RedisHBaseConfiguration.stringValueExpireSeconds() * 1000L;
        if (expireMillis == 0) expireMillis = 1L;
        return Math.min(RedisHBaseConfiguration.stringValueExpireSeconds() * 1000L, expireMillis);
    }

    public static final int HASH_REF_KEY_THRESHOLD_MIN = 32;//阈值最小值是32
    public static int hashRefKeyThreshold() {
        int threshold = RedisHBaseConfiguration.hashMemberRefKeyThreshold();
        return Math.max(threshold, HASH_REF_KEY_THRESHOLD_MIN);
    }

    public static int hashRefKeyExpireSeconds() {
        return RedisHBaseConfiguration.hashMemberRefKeyExpireSeconds();
    }

    public static byte[] hbaseGet(CamelliaHBaseTemplate hBaseTemplate, CamelliaRedisTemplate redisTemplate,
                             byte[] rowKey, int expireSeconds) {
        byte[] hbaseValue = null;
        if (RedisHBaseConfiguration.hbaseReadDegraded()) {
            logger.warn("get from hbase degraded, rowKey = {}", Bytes.toHex(rowKey));
            RedisHBaseMonitor.incrDegraded("hbase_read_degraded");
        } else {
            if (FreqUtils.hbaseReadFreq()) {
                Get get = new Get(rowKey);
                Result result = hBaseTemplate.get(hbaseTableName(), get);
                hbaseValue = result.getValue(CF_D, COL_DATA);
                if (hbaseValue != null) {
                    redisTemplate.setex(redisKey(rowKey), expireSeconds, hbaseValue);
                }
            } else {
                RedisHBaseMonitor.incrDegraded("hbase_read_freq_degraded");
            }
        }
        return hbaseValue;
    }

    public static void flushHBasePut(HBaseAsyncWriteExecutor hBaseAsyncWriteExecutor, CamelliaHBaseTemplate hBaseTemplate,
                                     byte[] key, byte[] rowKey, byte[] value, Long expire, String method) {
        List<Put> putList = new ArrayList<>();
        Put put = new Put(rowKey);
        put.addColumn(CF_D, COL_DATA, value);
        if (expire != null) {
            put.addColumn(CF_D, COL_EXPIRE, Bytes.toBytes(expire));
        }
        putList.add(put);
        if (RedisHBaseConfiguration.hbaseAsyncWriteEnable()) {
            HBaseAsyncWriteExecutor.HBaseAsyncWriteTask writeTask = new HBaseAsyncWriteExecutor.HBaseAsyncWriteTask();
            writeTask.setKey(key);
            writeTask.setPuts(putList);
            boolean success = hBaseAsyncWriteExecutor.submit(writeTask);
            if (!success) {
                if (RedisHBaseConfiguration.hbaseDegradedIfAsyncWriteSubmitFail()) {
                    logger.error("hBaseAsyncWriteExecutor submit fail for {}, degraded hbase write, key = {}", method, Utils.bytesToString(key));
                    RedisHBaseMonitor.incrDegraded(method + "|async_write_submit_fail");
                } else {
                    logger.warn("hBaseAsyncWriteExecutor submit fail, write sync for {}, key = {}", method, Utils.bytesToString(key));
                    hBaseTemplate.put(hbaseTableName(), putList);
                }
            }
        } else {
            hBaseTemplate.put(hbaseTableName(), putList);
        }
    }
}
