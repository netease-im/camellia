package com.netease.nim.camellia.redis.proxy.hbase;

import com.netease.nim.camellia.hbase.CamelliaHBaseTemplate;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import com.netease.nim.camellia.redis.pipeline.ICamelliaRedisPipeline;
import com.netease.nim.camellia.redis.proxy.hbase.conf.RedisHBaseConfiguration;
import com.netease.nim.camellia.redis.proxy.hbase.monitor.OperationType;
import com.netease.nim.camellia.redis.proxy.hbase.monitor.RedisHBaseMonitor;
import com.netease.nim.camellia.redis.proxy.hbase.util.FreqUtils;
import com.netease.nim.camellia.redis.proxy.hbase.util.HBaseValue;
import com.netease.nim.camellia.redis.proxy.util.BytesKey;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Response;
import redis.clients.util.SafeEncoder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.netease.nim.camellia.redis.proxy.hbase.util.RedisHBaseUtils.*;

/**
 *
 * Created by caojiajun on 2021/7/6
 */
public class RedisHBaseStringMixClient {

    private static final Logger logger = LoggerFactory.getLogger(RedisHBaseStringMixClient.class);

    private final CamelliaRedisTemplate redisTemplate;
    private final CamelliaHBaseTemplate hBaseTemplate;
    private final HBaseAsyncWriteExecutor hBaseAsyncWriteExecutor;

    public RedisHBaseStringMixClient(CamelliaRedisTemplate redisTemplate,
                                     CamelliaHBaseTemplate hBaseTemplate,
                                     HBaseAsyncWriteExecutor hBaseAsyncWriteExecutor) {
        this.redisTemplate = redisTemplate;
        this.hBaseTemplate = hBaseTemplate;
        this.hBaseAsyncWriteExecutor = hBaseAsyncWriteExecutor;
    }

    /**
     * 缺陷：set完立马del，然后再get，如果是多台proxy，有可能del不掉
     */
    public Long del(byte[] key) {
        Response<Long> response;
        try (ICamelliaRedisPipeline pipeline = redisTemplate.pipelined()) {
            response = pipeline.del(redisKey(key));
            pipeline.del(nullCacheKey(key));
            pipeline.sync();
        }
        List<Delete> deleteList = new ArrayList<>();
        Delete delete = new Delete(hbaseRowKey(key));
        deleteList.add(delete);
        if (RedisHBaseConfiguration.hbaseAsyncWriteEnable()) {
            HBaseAsyncWriteExecutor.HBaseAsyncWriteTask writeTask = new HBaseAsyncWriteExecutor.HBaseAsyncWriteTask();
            writeTask.setKey(key);
            writeTask.setDeletes(deleteList);
            boolean success = hBaseAsyncWriteExecutor.submit(writeTask);
            if (!success) {
                if (RedisHBaseConfiguration.hbaseDegradedIfAsyncWriteSubmitFail()) {
                    logger.error("hBaseAsyncWriteExecutor submit fail for string_del, degraded for hbase write, key = {}", Utils.bytesToString(key));
                    RedisHBaseMonitor.incrDegraded("string_del|async_write_submit_fail");
                } else {
                    logger.warn("hBaseAsyncWriteExecutor submit fail, write sync for string_del, key = {}", Utils.bytesToString(key));
                    hBaseTemplate.delete(hbaseTableName(), deleteList);
                }
            }
        } else {
            hBaseTemplate.delete(hbaseTableName(), delete);
        }
        return response.get();
    }

    public Long pttl(byte[] key) {
        Long pttl = redisTemplate.pttl(redisKey(key));
        byte[] rowKey = hbaseRowKey(key);
        HBaseValue hBaseValue = null;
        if (RedisHBaseConfiguration.hbaseReadDegraded()) {
            logger.warn("get from hbase degraded, rowKey = {}", Bytes.toHex(rowKey));
            RedisHBaseMonitor.incrDegraded("hbase_read_degraded");
        } else {
            if (FreqUtils.hbaseReadFreq()) {
                Get get = new Get(rowKey);
                Result result = hBaseTemplate.get(hbaseTableName(), get);
                hBaseValue = parseOriginalValueCheckExpire(result);
            } else {
                RedisHBaseMonitor.incrDegraded("hbase_read_freq_degraded");
            }
        }
        if (hBaseValue == null) {
            return pttl;
        }
        if (hBaseValue.getValue() != null && !hBaseValue.isExpire() && hBaseValue.getTtlMillis() == null) {
            return -1L;
        }
        Long ttlMillis = hBaseValue.getTtlMillis();
        if (hBaseValue.isExpire()) {
            return -2L;
        }
        return ttlMillis;
    }

    public Long pexpireAt(byte[] key, long millisecondsTimestamp) {
        byte[] bytes = get(key);
        if (bytes != null) {
            if (millisecondsTimestamp - System.currentTimeMillis() > 0) {
                psetex(key, millisecondsTimestamp - System.currentTimeMillis(), bytes);
            } else {
                del(key);
            }
            return 1L;
        } else {
            return 0L;
        }
    }

    public Long exists(byte[] key) {
        byte[] bytes = get(key);
        if (bytes != null) {
            return 1L;
        }
        return 0L;
    }

    public String set(byte[] key, byte[] value) {
        try {
            if (value.length > stringValueThreshold()) {
                String setex = redisTemplate.setex(redisKey(key), (int)(stringValueExpireMillis(null) / 1000L), value);
                flushHBasePut(hBaseAsyncWriteExecutor, hBaseTemplate, key, hbaseRowKey(key), value, -1L, "set");
                RedisHBaseMonitor.incr("set(byte[], byte[])", OperationType.REDIS_HBASE.name());
                RedisHBaseMonitor.incrValueSize("string", value.length, true);
                return setex;
            } else {
                RedisHBaseMonitor.incr("set(byte[], byte[])", OperationType.REDIS_ONLY.name());
                RedisHBaseMonitor.incrValueSize("string", value.length, false);
                return redisTemplate.set(redisKey(key), value);
            }
        } finally {
            redisTemplate.del(nullCacheKey(key));
        }
    }

    public String mset(byte[]... keysvalues) {
        List<byte[]> list = new ArrayList<>();
        int pipelineCount = 0;
        boolean hitHBase = false;
        try (ICamelliaRedisPipeline pipeline = redisTemplate.pipelined()) {
            for (int i = 0; i < keysvalues.length; i += 2) {
                byte[] key = keysvalues[i];
                byte[] value = keysvalues[i + 1];
                if (value.length > stringValueThreshold()) {
                    pipeline.setex(redisKey(key), (int)(stringValueExpireMillis(null) / 1000L), value);
                    flushHBasePut(hBaseAsyncWriteExecutor, hBaseTemplate, key, hbaseRowKey(key), value, -1L,"mset");
                    pipelineCount ++;
                    hitHBase = true;
                    RedisHBaseMonitor.incrValueSize("string", value.length, true);
                } else {
                    list.add(redisKey(key));
                    list.add(value);
                    RedisHBaseMonitor.incrValueSize("string", value.length, false);
                }
                pipeline.del(nullCacheKey(key));
                pipelineCount ++;
                if (pipelineCount >= RedisHBaseConfiguration.redisMaxPipeline()) {
                    pipeline.sync();
                    pipelineCount = 0;
                }
            }
            pipeline.sync();
        }
        if (hitHBase) {
            RedisHBaseMonitor.incr("mset(byte[][])", OperationType.REDIS_HBASE.name());
        } else {
            RedisHBaseMonitor.incr("mset(byte[][])", OperationType.REDIS_ONLY.name());
        }
        if (!list.isEmpty()) {
            return redisTemplate.mset(list.toArray(new byte[0][0]));
        } else {
            return "OK";
        }
    }

    public byte[] get(byte[] key) {
        byte[] value = redisTemplate.get(redisKey(key));
        if (value == null) {
            byte[] nullCacheKey = nullCacheKey(key);
            Boolean nullCache = redisTemplate.exists(nullCacheKey);
            if (nullCache) {
                RedisHBaseMonitor.incr("get(byte[])", OperationType.REDIS_ONLY.name());
                return null;
            }
            byte[] rowKey = hbaseRowKey(key);
            HBaseValue hBaseValue = null;
            if (RedisHBaseConfiguration.hbaseReadDegraded()) {
                logger.warn("get from hbase degraded, rowKey = {}", Bytes.toHex(rowKey));
                RedisHBaseMonitor.incrDegraded("hbase_read_degraded");
            } else {
                if (FreqUtils.hbaseReadFreq()) {
                    Get get = new Get(rowKey);
                    Result result = hBaseTemplate.get(hbaseTableName(), get);
                    hBaseValue = parseOriginalValueCheckExpire(result);
                } else {
                    RedisHBaseMonitor.incrDegraded("hbase_read_freq_degraded");
                }
            }
            if (hBaseValue != null) {
                value = hBaseValue.getValue();
                RedisHBaseMonitor.incr("get(byte[])", OperationType.REDIS_HBASE.name());
                if (value != null && !hBaseValue.isExpire()) {
                    redisTemplate.setex(redisKey(key), (int)(stringValueExpireMillis(hBaseValue.getTtlMillis()) / 1000L), value);
                } else {
                    value = null;
                    redisTemplate.setex(nullCacheKey, (int)(stringValueExpireMillis(null) / 1000L) / 2, SafeEncoder.encode("1"));
                }
            }
        } else {
            RedisHBaseMonitor.incr("get(byte[])", OperationType.REDIS_ONLY.name());
        }
        return value;
    }

    public String setex(byte[] key, int seconds, byte[] value) {
        try {
            if (value.length > stringValueThreshold()) {
                String setex = redisTemplate.setex(redisKey(key), (int)(stringValueExpireMillis(seconds * 1000L) / 1000L), value);
                flushHBasePut(hBaseAsyncWriteExecutor, hBaseTemplate, key, hbaseRowKey(key), value, System.currentTimeMillis() + seconds * 1000L,"setex");
                RedisHBaseMonitor.incr("setex(byte[], byte[])", OperationType.REDIS_HBASE.name());
                RedisHBaseMonitor.incrValueSize("string", value.length, true);
                return setex;
            } else {
                RedisHBaseMonitor.incr("setex(byte[], byte[])", OperationType.REDIS_ONLY.name());
                RedisHBaseMonitor.incrValueSize("string", value.length, false);
                return redisTemplate.setex(redisKey(key), seconds, value);
            }
        } finally {
            redisTemplate.del(nullCacheKey(key));
        }
    }

    public String psetex(byte[] key, long milliseconds, byte[] value) {
        try {
            if (value.length > stringValueThreshold()) {
                String psetex = redisTemplate.psetex(redisKey(key), stringValueExpireMillis(milliseconds), value);
                flushHBasePut(hBaseAsyncWriteExecutor, hBaseTemplate, key, hbaseRowKey(key), value, System.currentTimeMillis() + milliseconds,"psetx");
                RedisHBaseMonitor.incr("psetex(byte[], byte[])", OperationType.REDIS_HBASE.name());
                RedisHBaseMonitor.incrValueSize("string", value.length, true);
                return psetex;
            } else {
                RedisHBaseMonitor.incr("psetex(byte[], byte[])", OperationType.REDIS_ONLY.name());
                RedisHBaseMonitor.incrValueSize("string", value.length, false);
                return redisTemplate.psetex(redisKey(key), milliseconds, value);
            }
        } finally {
            redisTemplate.del(nullCacheKey(key));
        }
    }

    public List<byte[]> mget(byte[]... keys) {
        List<byte[]> list = new ArrayList<>(keys.length);
        for (byte[] key : keys) {
            list.add(redisKey(key));
        }
        List<byte[]> mget = redisTemplate.mget(list.toArray(new byte[0][0]));
        Map<Integer, byte[]> valueMap = new HashMap<>();
        Map<Integer, byte[]> missKeyMap = new HashMap<>();
        Map<BytesKey, Integer> missKeyReverseMap = new HashMap<>();
        for (int i=0; i<mget.size(); i++) {
            byte[] key = keys[i];
            byte[] value = mget.get(i);
            if (value != null) {
                valueMap.put(i, value);
            } else {
                missKeyMap.put(i, key);
            }
            missKeyReverseMap.put(new BytesKey(key), i);
        }
        if (missKeyMap.isEmpty()) {
            RedisHBaseMonitor.incr("mget(byte[][])", OperationType.REDIS_ONLY.name());
            return toList(valueMap, keys.length);
        }
        List<Response<Boolean>> nullCacheList = new ArrayList<>();
        List<byte[]> originalKeys1 = new ArrayList<>();
        try (ICamelliaRedisPipeline pipeline = redisTemplate.pipelined()) {
            int pipelineCount = 0;
            for (byte[] key : missKeyMap.values()) {
                Response<Boolean> exists = pipeline.exists(nullCacheKey(key));
                nullCacheList.add(exists);
                originalKeys1.add(key);
                pipelineCount ++;
                if (pipelineCount >= RedisHBaseConfiguration.redisMaxPipeline()) {
                    pipeline.sync();
                    pipelineCount = 0;
                }
            }
            pipeline.sync();
        }
        for (int i=0; i<nullCacheList.size(); i++) {
            Boolean nullCache = nullCacheList.get(i).get();
            if (nullCache) {
                byte[] originalKey = originalKeys1.get(i);
                Integer index = missKeyReverseMap.get(new BytesKey(originalKey));
                if (index != null) {
                    valueMap.put(index, null);
                    missKeyMap.remove(index);
                }
            }
        }
        if (missKeyMap.isEmpty()) {
            RedisHBaseMonitor.incr("mget(byte[][])", OperationType.REDIS_ONLY.name());
            return toList(valueMap, keys.length);
        }
        List<Get> gets = new ArrayList<>();
        List<byte[]> originalKeys2 = new ArrayList<>();
        for (byte[] key : missKeyMap.values()) {
            Get get = new Get(hbaseRowKey(key));
            gets.add(get);
            originalKeys2.add(key);
        }
        Map<BytesKey, byte[]> hbaseMap = new HashMap<>();
        if (RedisHBaseConfiguration.hbaseReadDegraded()) {
            List<String> hbaseRows = new ArrayList<>();
            for (Get get : gets) {
                hbaseRows.add(Bytes.toHex(get.getRow()));
            }
            logger.warn("mget from hbase degraded, rowKeys = {}", hbaseRows);
            RedisHBaseMonitor.incrDegraded("hbase_read_batch_degraded");
        } else {
            if (FreqUtils.hbaseReadFreq()) {
                Result[] results = hBaseTemplate.get(hbaseTableName(), gets);
                try (ICamelliaRedisPipeline pipeline = redisTemplate.pipelined()) {
                    int pipelineCount = 0;
                    for (int i=0; i<results.length; i++) {
                        Result result = results[i];
                        HBaseValue hBaseValue = parseOriginalValueCheckExpire(result);
                        if (hBaseValue == null) continue;
                        byte[] value = hBaseValue.getValue();
                        byte[] originalKey = originalKeys2.get(i);
                        if (value != null && !hBaseValue.isExpire()) {
                            hbaseMap.put(new BytesKey(originalKey), value);
                            pipeline.setex(redisKey(originalKey), (int)(stringValueExpireMillis(hBaseValue.getTtlMillis()) / 1000L), value);
                        } else {
                            pipeline.setex(nullCacheKey(originalKey), (int)(stringValueExpireMillis(null) / 1000L) / 2, SafeEncoder.encode("1"));
                        }
                        pipelineCount ++;
                        if (pipelineCount >= RedisHBaseConfiguration.redisMaxPipeline()) {
                            pipeline.sync();
                            pipelineCount = 0;
                        }
                    }
                    pipeline.sync();
                }
            } else {
                RedisHBaseMonitor.incrDegraded("hbase_read_batch_freq_degraded");
            }
        }
        for (Map.Entry<Integer, byte[]> entry : missKeyMap.entrySet()) {
            byte[] key = entry.getValue();
            byte[] value = hbaseMap.get(new BytesKey(key));
            valueMap.put(entry.getKey(), value);
        }
        RedisHBaseMonitor.incr("mget(byte[][])", OperationType.REDIS_HBASE.name());
        return toList(valueMap, keys.length);
    }

    private List<byte[]> toList(Map<Integer, byte[]> map, int len) {
        List<byte[]> list = new ArrayList<>();
        for (int i=0; i<len; i++) {
            byte[] bytes = map.get(i);
            list.add(bytes);
        }
        return list;
    }
}
