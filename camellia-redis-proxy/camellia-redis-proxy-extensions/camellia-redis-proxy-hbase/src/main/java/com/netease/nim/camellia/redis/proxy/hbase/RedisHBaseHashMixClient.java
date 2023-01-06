package com.netease.nim.camellia.redis.proxy.hbase;

import com.netease.nim.camellia.hbase.CamelliaHBaseTemplate;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import com.netease.nim.camellia.redis.pipeline.ICamelliaRedisPipeline;
import com.netease.nim.camellia.redis.proxy.hbase.conf.RedisHBaseConfiguration;
import com.netease.nim.camellia.redis.proxy.hbase.monitor.OperationType;
import com.netease.nim.camellia.redis.proxy.hbase.monitor.RedisHBaseMonitor;
import com.netease.nim.camellia.redis.proxy.hbase.util.FreqUtils;
import com.netease.nim.camellia.tools.utils.BytesKey;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Response;
import redis.clients.util.SafeEncoder;

import java.util.*;

import static com.netease.nim.camellia.redis.proxy.hbase.util.RedisHBaseUtils.*;

/**
 *
 * Created by caojiajun on 2021/7/6
 */
public class RedisHBaseHashMixClient {

    private static final Logger logger = LoggerFactory.getLogger(RedisHBaseHashMixClient.class);

    private final CamelliaRedisTemplate redisTemplate;
    private final CamelliaHBaseTemplate hBaseTemplate;
    private final HBaseAsyncWriteExecutor hBaseAsyncWriteExecutor;

    public RedisHBaseHashMixClient(CamelliaRedisTemplate redisTemplate,
                                   CamelliaHBaseTemplate hBaseTemplate,
                                   HBaseAsyncWriteExecutor hBaseAsyncWriteExecutor) {
        this.redisTemplate = redisTemplate;
        this.hBaseTemplate = hBaseTemplate;
        this.hBaseAsyncWriteExecutor = hBaseAsyncWriteExecutor;
    }

    public Long del(byte[] key) {
        List<byte[]> hvals = redisTemplate.hvals(redisKey(key));
        List<Delete> deleteList = new ArrayList<>();
        try (ICamelliaRedisPipeline pipeline = redisTemplate.pipelined()) {
            int pipelineCount = 0;
            for (byte[] value : hvals) {
                if (isRefKey(key, value)) {
                    Delete delete = new Delete(value);
                    deleteList.add(delete);
                    pipeline.del(redisKey(value));
                    pipelineCount ++;
                    if (pipelineCount >= RedisHBaseConfiguration.redisMaxPipeline()) {
                        pipeline.sync();
                        pipelineCount = 0;
                    }
                }
            }
            pipeline.sync();
        }
        if (!deleteList.isEmpty()) {
            if (RedisHBaseConfiguration.hbaseAsyncWriteEnable()) {
                HBaseAsyncWriteExecutor.HBaseAsyncWriteTask writeTask = new HBaseAsyncWriteExecutor.HBaseAsyncWriteTask();
                writeTask.setKey(key);
                writeTask.setDeletes(deleteList);
                boolean success = hBaseAsyncWriteExecutor.submit(writeTask);
                if (!success) {
                    if (RedisHBaseConfiguration.hbaseDegradedIfAsyncWriteSubmitFail()) {
                        logger.error("hBaseAsyncWriteExecutor submit fail for hash_del, degraded for hbase write, key = {}", Utils.bytesToString(key));
                        RedisHBaseMonitor.incrDegraded("hash_del|async_write_submit_fail");
                    } else {
                        logger.warn("hBaseAsyncWriteExecutor submit fail, write sync for hash_del, key = {}", Utils.bytesToString(key));
                        hBaseTemplate.delete(hbaseTableName(), deleteList);
                    }
                }
            } else {
                hBaseTemplate.delete(hbaseTableName(), deleteList);
            }
            RedisHBaseMonitor.incr("hash_del(byte[])", OperationType.REDIS_HBASE.name());
        } else {
            RedisHBaseMonitor.incr("hash_del(byte[])", OperationType.REDIS_ONLY.name());
        }
        return redisTemplate.del(redisKey(key));
    }

    public Long hset(byte[] key, byte[] field, byte[] value) {
        if (value.length > hashRefKeyThreshold()) {
            byte[] refKey = buildRefKey(key, value);
            Response<Long> response;
            try (ICamelliaRedisPipeline pipeline = redisTemplate.pipelined()) {
                response = pipeline.hset(redisKey(key), field, refKey);
                pipeline.setex(redisKey(refKey), hashRefKeyExpireSeconds(), value);
                pipeline.sync();
            }
            flushHBasePut(hBaseAsyncWriteExecutor, hBaseTemplate, key, refKey, value, null, "hset");
            RedisHBaseMonitor.incr("hset(byte[], byte[], byte[])", OperationType.REDIS_HBASE.name());
            RedisHBaseMonitor.incrValueSize("hash", value.length, true);
            return response.get();
        } else {
            RedisHBaseMonitor.incr("hset(byte[], byte[], byte[])", OperationType.REDIS_ONLY.name());
            RedisHBaseMonitor.incrValueSize("hash", value.length, false);
            return redisTemplate.hset(redisKey(key), field, value);
        }
    }

    public Long hsetnx(byte[] key, byte[] field, byte[] value) {
        if (value.length > hashRefKeyThreshold()) {
            byte[] refKey = buildRefKey(key, value);
            Long hsetnx = redisTemplate.hsetnx(redisKey(key), field, refKey);
            if (hsetnx > 0) {
                redisTemplate.setex(redisKey(refKey), hashRefKeyExpireSeconds(), value);
                flushHBasePut(hBaseAsyncWriteExecutor, hBaseTemplate, key, refKey, value, null, "hsetnx");
            }
            RedisHBaseMonitor.incr("hsetnx(byte[], byte[], byte[])", OperationType.REDIS_HBASE.name());
            RedisHBaseMonitor.incrValueSize("hash", value.length, true);
            return hsetnx;
        } else {
            RedisHBaseMonitor.incr("hsetnx(byte[], byte[], byte[])", OperationType.REDIS_ONLY.name());
            RedisHBaseMonitor.incrValueSize("hash", value.length, false);
            return redisTemplate.hsetnx(redisKey(key), field, value);
        }
    }

    public byte[] hget(byte[] key, byte[] field) {
        byte[] bytes = redisTemplate.hget(redisKey(key), field);
        if (bytes == null) {
            RedisHBaseMonitor.incr("hget(byte[], byte[])", OperationType.REDIS_ONLY.name());
            return bytes;
        }
        if (isRefKey(key, bytes)) {
            byte[] value = redisTemplate.get(redisKey(bytes));
            if (value != null) {
                RedisHBaseMonitor.incr("hget(byte[], byte[])", OperationType.REDIS_ONLY.name());
                return value;
            }
            value = hbaseGet(hBaseTemplate, redisTemplate, bytes, hashRefKeyExpireSeconds());
            RedisHBaseMonitor.incr("hget(byte[], byte[])", OperationType.REDIS_HBASE.name());
            return value;
        } else {
            RedisHBaseMonitor.incr("hget(byte[], byte[])", OperationType.REDIS_ONLY.name());
            return bytes;
        }
    }

    public Boolean hexists(byte[] key, byte[] field) {
        RedisHBaseMonitor.incr("hexists(byte[], byte[])", OperationType.REDIS_ONLY.name());
        return redisTemplate.hexists(redisKey(key), field);
    }

    public Long hdel(byte[] key, byte[]... field) {
        RedisHBaseMonitor.incr("hdel(byte[], byte[][])", OperationType.REDIS_ONLY.name());
        return redisTemplate.hdel(redisKey(key), field);
    }

    public Long hlen(byte[] key) {
        RedisHBaseMonitor.incr("hlen(byte[])", OperationType.REDIS_ONLY.name());
        return redisTemplate.hlen(redisKey(key));
    }

    public Set<byte[]> hkeys(byte[] key) {
        RedisHBaseMonitor.incr("hkeys(byte[])", OperationType.REDIS_ONLY.name());
        return redisTemplate.hkeys(redisKey(key));
    }

    public String hmset(byte[] key, Map<byte[], byte[]> hash) {
        Map<byte[], byte[]> map = new HashMap<>();
        Map<byte[], byte[]> refKeyMap = new HashMap<>();
        for (Map.Entry<byte[], byte[]> entry : hash.entrySet()) {
            byte[] k = entry.getKey();
            byte[] v = entry.getValue();
            if (v.length > hashRefKeyThreshold()) {
                byte[] refKey = buildRefKey(key, v);
                map.put(k, refKey);
                refKeyMap.put(refKey, v);
                RedisHBaseMonitor.incrValueSize("hash", v.length, true);
            } else {
                map.put(k, v);
                RedisHBaseMonitor.incrValueSize("hash", v.length, false);
            }
        }
        if (refKeyMap.isEmpty()) {
            RedisHBaseMonitor.incr("hmset(byte[], Map)", OperationType.REDIS_ONLY.name());
            return redisTemplate.hmset(redisKey(key), map);
        } else {
            List<Put> putList = new ArrayList<>();
            Response<String> response;
            int pipelineCount = 0;
            try (ICamelliaRedisPipeline pipeline = redisTemplate.pipelined()) {
                for (Map.Entry<byte[], byte[]> entry : refKeyMap.entrySet()) {
                    pipeline.setex(redisKey(entry.getKey()), hashRefKeyExpireSeconds(), entry.getValue());
                    Put put = new Put(entry.getKey());
                    put.addColumn(CF_D, COL_DATA, entry.getValue());
                    putList.add(put);
                    pipelineCount ++;
                    if (pipelineCount >= RedisHBaseConfiguration.redisMaxPipeline()) {
                        pipeline.sync();
                        pipelineCount = 0;
                    }
                }
                pipeline.sync();
                response = pipeline.hmset(redisKey(key), map);
                pipeline.sync();
            }
            List<List<Put>> split = split(putList, RedisHBaseConfiguration.hbaseMaxBatch());
            for (List<Put> puts : split) {
                HBaseAsyncWriteExecutor.HBaseAsyncWriteTask task = new HBaseAsyncWriteExecutor.HBaseAsyncWriteTask();
                task.setKey(key);
                task.setPuts(puts);
                boolean success = hBaseAsyncWriteExecutor.submit(task);
                if (!success) {
                    if (RedisHBaseConfiguration.hbaseDegradedIfAsyncWriteSubmitFail()) {
                        logger.error("hBaseAsyncWriteExecutor submit fail for hmset, degraded hbase write, key = {}", Utils.bytesToString(key));
                        RedisHBaseMonitor.incrDegraded("hmset|async_write_submit_fail");
                    } else {
                        logger.warn("hBaseAsyncWriteExecutor submit fail, write sync for hmset, key = {}", Utils.bytesToString(key));
                        hBaseTemplate.put(hbaseTableName(), putList);
                    }
                }
            }
            RedisHBaseMonitor.incr("hmset(byte[], Map)", OperationType.REDIS_HBASE.name());
            return response.get();
        }
    }

    public List<byte[]> hmget(byte[] key, byte[]... fields) {
        List<byte[]> list = redisTemplate.hmget(redisKey(key), fields);
        return parseList(key, list, "hmget(byte[], byte[][])", false);
    }

    public List<byte[]> hvals(byte[] key) {
        List<byte[]> list = redisTemplate.hvals(redisKey(key));
        return parseList(key, list, "hvals(byte[])", true);
    }

    public Map<byte[], byte[]> hgetAll(byte[] key) {
        Map<byte[], byte[]> map = redisTemplate.hgetAll(redisKey(key));
        if (map == null || map.isEmpty()) {
            RedisHBaseMonitor.incr("hgetAll(byte[])", OperationType.REDIS_ONLY.name());
            return map;
        }
        Map<byte[], byte[]> ret = new HashMap<>();
        Map<BytesKey, Response<byte[]>> redisMap = new HashMap<>();
        Map<BytesKey, BytesKey> missMap = new HashMap<>();
        int pipelineCount = 0;
        try (ICamelliaRedisPipeline pipeline = redisTemplate.pipelined()) {
            for (Map.Entry<byte[], byte[]> entry : map.entrySet()) {
                byte[] field = entry.getKey();
                byte[] value = entry.getValue();
                if (isRefKey(key, value)) {
                    Response<byte[]> response = pipeline.get(redisKey(value));
                    pipelineCount ++;
                    if (pipelineCount >= RedisHBaseConfiguration.redisMaxPipeline()) {
                        pipeline.sync();
                        pipelineCount = 0;
                    }
                    BytesKey bytesKey = new BytesKey(field);
                    redisMap.put(bytesKey, response);
                    missMap.put(bytesKey, new BytesKey(value));
                } else {
                    ret.put(field, value);
                }
            }
            pipeline.sync();
        }
        if (missMap.isEmpty()) {
            RedisHBaseMonitor.incr("hgetAll(byte[])", OperationType.REDIS_ONLY.name());
            return ret;
        }
        for (Map.Entry<BytesKey, Response<byte[]>> entry : redisMap.entrySet()) {
            byte[] bytes = entry.getValue().get();
            if (bytes != null) {
                ret.put(entry.getKey().getKey(), bytes);
                missMap.remove(entry.getKey());
            }
        }
        if (missMap.isEmpty()) {
            RedisHBaseMonitor.incr("hgetAll(byte[])", OperationType.REDIS_ONLY.name());
            return ret;
        }
        RedisHBaseMonitor.incr("hgetAll(byte[])", OperationType.REDIS_HBASE.name());
        if (RedisHBaseConfiguration.hbaseReadDegraded()) {
            logger.warn("hgetAll from hbase degraded, key = {}", SafeEncoder.encode(key));
            RedisHBaseMonitor.incrDegraded("hbase_read_batch_degraded");
        } else {
            if (FreqUtils.hbaseReadFreq()) {
                List<byte[]> fieldList = new ArrayList<>();
                List<Get> getList = new ArrayList<>();
                for (Map.Entry<BytesKey, BytesKey> entry : missMap.entrySet()) {
                    BytesKey field = entry.getKey();
                    fieldList.add(field.getKey());
                    getList.add(new Get(entry.getValue().getKey()));
                }
                Result[] results = hBaseTemplate.get(hbaseTableName(), getList);
                pipelineCount = 0;
                try (ICamelliaRedisPipeline pipeline = redisTemplate.pipelined()) {
                    for (int i = 0; i < results.length; i++) {
                        Result result = results[i];
                        byte[] value = result.getValue(CF_D, COL_DATA);
                        if (value != null) {
                            byte[] field = fieldList.get(i);
                            ret.put(field, value);
                            pipeline.setex(redisKey(getList.get(i).getRow()), hashRefKeyExpireSeconds(), value);
                            pipelineCount ++;
                            if (pipelineCount >= RedisHBaseConfiguration.redisMaxPipeline()) {
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
        return ret;
    }

    private List<byte[]> parseList(byte[] key, List<byte[]> list, String method, boolean skipNull) {
        if (list == null || list.isEmpty()) {
            RedisHBaseMonitor.incr(method, OperationType.REDIS_ONLY.name());
            return list;
        }
        Map<Integer, byte[]> map = new HashMap<>();
        Map<Integer, Response<byte[]>> pipelineMap = new HashMap<>();
        try (ICamelliaRedisPipeline pipeline = redisTemplate.pipelined()) {
            int pipelineCount = 0;
            for (int i=0; i<list.size(); i++) {
                byte[] bytes = list.get(i);
                if (bytes == null) {
                    if (!skipNull) {
                        map.put(i, null);
                    }
                    continue;
                }
                if (isRefKey(key, bytes)) {
                    Response<byte[]> response = pipeline.get(redisKey(bytes));
                    pipelineMap.put(i, response);
                    pipelineCount ++;
                    if (pipelineCount >= RedisHBaseConfiguration.redisMaxPipeline()) {
                        pipeline.sync();
                        pipelineCount = 0;
                    }
                } else {
                    map.put(i, bytes);
                }
            }
            pipeline.sync();
        }
        if (pipelineMap.isEmpty()) {
            RedisHBaseMonitor.incr(method, OperationType.REDIS_ONLY.name());
            return toList(map, list.size(), skipNull);
        }
        Map<Integer, byte[]> missMap = new HashMap<>();
        for (Map.Entry<Integer, Response<byte[]>> entry : pipelineMap.entrySet()) {
            Response<byte[]> value = entry.getValue();
            byte[] bytes = value.get();
            if (bytes != null) {
                map.put(entry.getKey(), bytes);
            } else {
                missMap.put(entry.getKey(), list.get(entry.getKey()));
            }
        }
        if (missMap.isEmpty()) {
            RedisHBaseMonitor.incr(method, OperationType.REDIS_ONLY.name());
            return toList(map, list.size(), skipNull);
        }
        List<Integer> list1 = new ArrayList<>();
        List<Get> getList = new ArrayList<>();
        for (Map.Entry<Integer, byte[]> entry : missMap.entrySet()) {
            list1.add(entry.getKey());
            getList.add(new Get(entry.getValue()));
        }
        if (RedisHBaseConfiguration.hbaseReadDegraded()) {
            logger.warn("{} from hbase degraded, key = {}", method, SafeEncoder.encode(key));
            RedisHBaseMonitor.incrDegraded("hbase_read_batch_degraded");
        } else {
            if (FreqUtils.hbaseReadFreq()) {
                Result[] results = hBaseTemplate.get(hbaseTableName(), getList);
                int pipelineCount = 0;
                try (ICamelliaRedisPipeline pipeline = redisTemplate.pipelined()) {
                    for (int i = 0; i < results.length; i++) {
                        Result result = results[i];
                        byte[] value = result.getValue(CF_D, COL_DATA);
                        if (value != null) {
                            pipeline.setex(redisKey(getList.get(i).getRow()), hashRefKeyExpireSeconds(), value);
                            Integer index = list1.get(i);
                            map.put(index, value);
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
        RedisHBaseMonitor.incr(method, OperationType.REDIS_HBASE.name());
        return toList(map, list.size(), skipNull);
    }


    private List<byte[]> toList(Map<Integer, byte[]> map, int len, boolean skipNull) {
        List<byte[]> list = new ArrayList<>();
        for (int i=0; i<len; i++) {
            byte[] bytes = map.get(i);
            if (bytes != null) {
                list.add(bytes);
            } else {
                if (!skipNull) {
                    list.add(null);
                }
            }
        }
        return list;
    }
}
