package com.netease.nim.camellia.redis.proxy.hbase;

import com.netease.nim.camellia.hbase.CamelliaHBaseTemplate;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import com.netease.nim.camellia.redis.pipeline.ICamelliaRedisPipeline;
import com.netease.nim.camellia.redis.proxy.hbase.conf.RedisHBaseConfiguration;
import com.netease.nim.camellia.redis.proxy.hbase.monitor.OperationType;
import com.netease.nim.camellia.redis.proxy.hbase.monitor.RedisHBaseMonitor;
import com.netease.nim.camellia.redis.proxy.hbase.util.FreqUtils;
import com.netease.nim.camellia.redis.proxy.util.BytesKey;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Response;

import java.util.*;

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

    public Long del(byte[] key) {
        byte[] bytes = redisTemplate.get(redisKey(key));
        if (bytes == null) return 0L;
        if (isRefKey(key, bytes)) {
            redisTemplate.del(redisKey(bytes));
            List<Delete> deleteList = new ArrayList<>();
            Delete delete = new Delete(bytes);
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
            RedisHBaseMonitor.incr("string_del(byte[])", OperationType.REDIS_HBASE.name());
        } else {
            RedisHBaseMonitor.incr("string_del(byte[])", OperationType.REDIS_ONLY.name());
        }
        return redisTemplate.del(redisKey(key));
    }

    public String set(byte[] key, byte[] value) {
        if (value.length > stringRefKeyThreshold()) {
            Response<String> response;
            byte[] refKey = buildRefKey(key, value);
            try (ICamelliaRedisPipeline pipeline = redisTemplate.pipelined()) {
                response = pipeline.set(redisKey(key), refKey);
                pipeline.setex(redisKey(refKey), stringRefKeyExpireSeconds(null), value);
                pipeline.sync();
            }
            flushHBasePut(hBaseAsyncWriteExecutor, hBaseTemplate, key, refKey, value, "set");
            RedisHBaseMonitor.incr("set(byte[], byte[])", OperationType.REDIS_HBASE.name());
            return response.get();
        } else {
            RedisHBaseMonitor.incr("set(byte[], byte[])", OperationType.REDIS_ONLY.name());
            return redisTemplate.set(redisKey(key), value);
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
                if (value.length > stringRefKeyThreshold()) {
                    byte[] refKey = buildRefKey(key, value);
                    pipeline.setex(redisKey(refKey), stringRefKeyExpireSeconds(null), value);
                    flushHBasePut(hBaseAsyncWriteExecutor, hBaseTemplate, key, refKey, value, "mset");
                    pipelineCount ++;
                    if (pipelineCount >= 100) {
                        pipeline.sync();
                        pipelineCount = 0;
                    }
                    list.add(redisKey(key));
                    list.add(refKey);
                    hitHBase = true;
                } else {
                    list.add(redisKey(key));
                    list.add(value);
                }
            }
            pipeline.sync();
        }
        if (hitHBase) {
            RedisHBaseMonitor.incr("mset(byte[][])", OperationType.REDIS_HBASE.name());
        } else {
            RedisHBaseMonitor.incr("mset(byte[][])", OperationType.REDIS_ONLY.name());
        }
        return redisTemplate.mset(list.toArray(new byte[0][0]));
    }

    public byte[] get(byte[] key) {
        byte[] value = redisTemplate.get(redisKey(key));
        if (value == null) {
            RedisHBaseMonitor.incr("get(byte[])", OperationType.REDIS_ONLY.name());
            return null;
        }
        if (isRefKey(key, value)) {
            byte[] bytes = redisTemplate.get(redisKey(value));
            if (bytes != null) {
                RedisHBaseMonitor.incr("get(byte[])", OperationType.REDIS_ONLY.name());
                return bytes;
            }
            bytes = hbaseGet(hBaseTemplate, redisTemplate, value, stringRefKeyExpireSeconds(null));
            RedisHBaseMonitor.incr("get(byte[])", OperationType.REDIS_HBASE.name());
            return bytes;
        } else {
            RedisHBaseMonitor.incr("get(byte[])", OperationType.REDIS_ONLY.name());
            return value;
        }
    }

    public String set(byte[] key, byte[] value, byte[] nxxx, byte[] expx, long time) {
        if (value.length > stringRefKeyThreshold()) {
            byte[] refKey = buildRefKey(key, value);
            String set = redisTemplate.set(redisKey(key), refKey, nxxx, expx, time);
            if (set.equalsIgnoreCase("ok")) {
                if (Utils.bytesToString(expx).equalsIgnoreCase("EX")) {
                    redisTemplate.setex(redisKey(refKey), stringRefKeyExpireSeconds((int)time), value);
                } else if (Utils.bytesToString(expx).equalsIgnoreCase("PX")) {
                    redisTemplate.setex(redisKey(refKey), stringRefKeyExpireSeconds((int)(time/1000)), value);
                } else {
                    redisTemplate.setex(redisKey(refKey), stringRefKeyExpireSeconds(null), value);
                }
                flushHBasePut(hBaseAsyncWriteExecutor, hBaseTemplate, key, refKey, value, "set");
            }
            RedisHBaseMonitor.incr("set(byte[], byte[], byte[], byte[], long)", OperationType.REDIS_HBASE.name());
            return set;
        } else {
            RedisHBaseMonitor.incr("set(byte[], byte[], byte[], byte[], long)", OperationType.REDIS_ONLY.name());
            return redisTemplate.set(redisKey(key), value);
        }
    }

    public Long setnx(byte[] key, byte[] value) {
        if (value.length > stringRefKeyThreshold()) {
            byte[] refKey = buildRefKey(key, value);
            Long setnx = redisTemplate.setnx(redisKey(key), refKey);
            if (setnx > 0) {
                redisTemplate.setex(redisKey(refKey), stringRefKeyExpireSeconds(null), value);
                flushHBasePut(hBaseAsyncWriteExecutor, hBaseTemplate, key, refKey, value, "setnx");
            }
            RedisHBaseMonitor.incr("setnx(byte[], byte[])", OperationType.REDIS_HBASE.name());
            return setnx;
        } else {
            RedisHBaseMonitor.incr("setnx(byte[], byte[])", OperationType.REDIS_ONLY.name());
            return redisTemplate.setnx(redisKey(key), value);
        }
    }

    public String setex(byte[] key, int seconds, byte[] value) {
        if (value.length > stringRefKeyThreshold()) {
            byte[] refKey = buildRefKey(key, value);
            String setex = redisTemplate.setex(redisKey(key), seconds, refKey);
            if (setex.equalsIgnoreCase("ok")) {
                redisTemplate.setex(redisKey(refKey), stringRefKeyExpireSeconds(seconds), value);
                flushHBasePut(hBaseAsyncWriteExecutor, hBaseTemplate, key, refKey, value, "setex");
            }
            RedisHBaseMonitor.incr("setex(byte[], byte[])", OperationType.REDIS_HBASE.name());
            return setex;
        } else {
            RedisHBaseMonitor.incr("setex(byte[], byte[])", OperationType.REDIS_ONLY.name());
            return redisTemplate.setex(redisKey(key), seconds, value);
        }
    }

    public String psetex(byte[] key, long milliseconds, byte[] value) {
        if (value.length > stringRefKeyThreshold()) {
            byte[] refKey = buildRefKey(key, value);
            String setex = redisTemplate.psetex(redisKey(key), milliseconds, refKey);
            if (setex.equalsIgnoreCase("ok")) {
                redisTemplate.setex(redisKey(refKey), stringRefKeyExpireSeconds((int)(milliseconds/ 1000)), value);
                flushHBasePut(hBaseAsyncWriteExecutor, hBaseTemplate, key, refKey, value, "psetx");
            }
            RedisHBaseMonitor.incr("psetex(byte[], byte[])", OperationType.REDIS_HBASE.name());
            return setex;
        } else {
            RedisHBaseMonitor.incr("psetex(byte[], byte[])", OperationType.REDIS_ONLY.name());
            return redisTemplate.psetex(redisKey(key), milliseconds, value);
        }
    }

    public List<byte[]> mget(byte[]... keys) {
        List<byte[]> list = new ArrayList<>(keys.length);
        for (byte[] key : keys) {
            list.add(redisKey(key));
        }
        List<byte[]> mget = redisTemplate.mget(list.toArray(new byte[0][0]));
        Map<Integer, byte[]> valueMap = new HashMap<>();
        Map<Integer, byte[]> missRefKey = new HashMap<>();
        Map<Integer, Response<byte[]>> missValue = new HashMap<>();
        int pipelineCount = 0;
        try (ICamelliaRedisPipeline pipeline = redisTemplate.pipelined()) {
            for (int i=0; i<mget.size(); i++) {
                byte[] key = keys[i];
                byte[] value = mget.get(i);
                if (value == null) {
                    valueMap.put(i, null);
                } else {
                    if (isRefKey(key, value)) {
                        Response<byte[]> response = pipeline.get(redisKey(value));
                        pipelineCount ++;
                        missRefKey.put(i, value);
                        missValue.put(i, response);
                        if (pipelineCount >= 100) {
                            pipeline.sync();
                            pipelineCount = 0;
                        }
                    } else {
                        valueMap.put(i, value);
                    }
                }
            }
            pipeline.sync();
        }
        if (!missValue.isEmpty()) {
            for (Map.Entry<Integer, Response<byte[]>> entry : missValue.entrySet()) {
                byte[] bytes = entry.getValue().get();
                if (bytes != null) {
                    valueMap.put(entry.getKey(), bytes);
                    missRefKey.remove(entry.getKey());
                }
            }
        }
        if (valueMap.size() == keys.length) {
            RedisHBaseMonitor.incr("mget(byte[][])", OperationType.REDIS_ONLY.name());
        } else {
            List<Get> gets = new ArrayList<>();
            for (byte[] value : missRefKey.values()) {
                Get get = new Get(value);
                gets.add(get);
            }
            Map<BytesKey, byte[]> hbaseMap = new HashMap<>();
            if (RedisHBaseConfiguration.hbaseReadDegraded()) {
                List<String> hbaseRows = new ArrayList<>();
                for (Get get : gets) {
                    hbaseRows.add(Bytes.toHex(get.getRow()));
                }
                logger.warn("mget from hbase degraded, keys = {}", hbaseRows);
                RedisHBaseMonitor.incrDegraded("hbase_read_batch_degraded");
            } else {
                if (FreqUtils.hbaseReadFreq()) {
                    Result[] results = hBaseTemplate.get(hbaseTableName(), gets);
                    try (ICamelliaRedisPipeline pipeline = redisTemplate.pipelined()) {
                        pipelineCount = 0;
                        for (Result result : results) {
                            byte[] value = result.getValue(CF_D, COL_DATA);
                            hbaseMap.put(new BytesKey(result.getRow()), value);
                            if (value != null) {
                                pipeline.setex(redisKey(result.getRow()), stringRefKeyExpireSeconds(null), value);
                                pipelineCount ++;
                                if (pipelineCount >= 100) {
                                    pipeline.sync();
                                    pipelineCount = 0;
                                }
                            }
                        }
                        pipeline.sync();
                    }
                } else {
                    RedisHBaseMonitor.incrDegraded("hbase_read_batch_freq_degraded");
                }
            }
            for (Map.Entry<Integer, byte[]> entry : missRefKey.entrySet()) {
                byte[] value = entry.getValue();
                Integer key = entry.getKey();
                byte[] bytes = hbaseMap.get(new BytesKey(value));
                valueMap.put(key, bytes);
            }
            RedisHBaseMonitor.incr("mget(byte[][])", OperationType.REDIS_HBASE.name());
        }
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
