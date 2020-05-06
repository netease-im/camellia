package com.netease.nim.camellia.redis.proxy.hbase;

import com.netease.nim.camellia.hbase.CamelliaHBaseTemplate;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import com.netease.nim.camellia.redis.pipeline.ICamelliaRedisPipeline;
import com.netease.nim.camellia.redis.proxy.hbase.conf.RedisHBaseConfiguration;
import com.netease.nim.camellia.redis.proxy.hbase.model.ExpireTask;
import com.netease.nim.camellia.redis.proxy.hbase.model.RedisHBaseType;
import com.netease.nim.camellia.redis.proxy.util.RedisKey;
import com.netease.nim.camellia.redis.toolkit.localcache.LocalCache;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Response;
import redis.clients.util.SafeEncoder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static com.netease.nim.camellia.redis.proxy.hbase.util.RedisHBaseUtil.*;

/**
 *
 * Created by caojiajun on 2020/3/5.
 */
public class RedisHBaseCommonMixClient {

    private static final Logger logger = LoggerFactory.getLogger(RedisHBaseCommonMixClient.class);

    private final CamelliaRedisTemplate redisTemplate;
    private final RedisHBaseZSetMixClient zSetMixClient;
    private final CamelliaHBaseTemplate hBaseTemplate;

    public RedisHBaseCommonMixClient(CamelliaRedisTemplate redisTemplate,
                                     CamelliaHBaseTemplate hBaseTemplate,
                                     RedisHBaseZSetMixClient zSetMixClient) {
        this.redisTemplate = redisTemplate;
        this.zSetMixClient = zSetMixClient;
        this.hBaseTemplate = hBaseTemplate;
        this.startAsyncExpireTask();
    }

    /**
     *
     */
    public Long del(byte[]... keys) {
        List<byte[]> zsetKeyList = new ArrayList<>();
        List<byte[]> otherTypeKeyList = new ArrayList<>();
        List<Response<String>> typeList = new ArrayList<>(keys.length);
        try (ICamelliaRedisPipeline pipeline = redisTemplate.pipelined()) {
            for (byte[] key : keys) {
                Response<String> type = pipeline.type(key);
                typeList.add(type);
            }
            pipeline.sync();
        }
        for (int i=0; i<keys.length; i++) {
            byte[] key = keys[i];
            String typeV = typeList.get(i).get();
            RedisHBaseType redisHBaseType = RedisHBaseType.byName(typeV);
            if (redisHBaseType == null) {
                redisHBaseType = redisHBaseTypeFromHBase(key);
            }
            if (redisHBaseType == null) {
                continue;
            }
            if (redisHBaseType == RedisHBaseType.ZSET) {
                zsetKeyList.add(key);
            } else {
                otherTypeKeyList.add(redisKey(key));
            }
        }
        Long ret = 0L;
        ret += zSetMixClient.del(zsetKeyList.toArray(new byte[0][0]));
        ret += redisTemplate.del(otherTypeKeyList.toArray(new byte[0][0]));
        return ret;
    }

    /**
     *
     */
    public String type(byte[] key) {
        RedisHBaseType redisHBaseType = redisHBaseType(key);
        if (redisHBaseType == null) {
            return "none";
        } else {
            return redisHBaseType.name().toLowerCase();
        }
    }

    /**
     *
     */
    public Long persist(byte[] key) {
        RedisHBaseType redisHBaseType = redisHBaseType(key);
        if (redisHBaseType == null) {
            return 0L;
        }
        Delete delete = new Delete(buildRowKey(key));
        delete.addColumns(CF_D, COL_EXPIRE_TIME);
        hBaseTemplate.delete(RedisHBaseConfiguration.hbaseTableName(), delete);
        return 1L;
    }

    /**
     *
     */
    public Long ttl(byte[] key) {
        Long pttl = pttl(key);
        if (pttl < 0) return pttl;
        return pttl / 1000L;
    }

    /**
     *
     */
    public Long pttl(byte[] key) {
        Result result = getFromHBase(key);
        if (result == null) {
            return -2L;
        }
        byte[] typeRaw = result.getValue(CF_D, COL_TYPE);
        if (typeRaw == null) {
            return -2L;
        }
        byte[] value = result.getValue(CF_D, COL_EXPIRE_TIME);
        if (value == null) {
            return -1L;
        }
        long pttl = Bytes.toLong(value) - System.currentTimeMillis();
        if (pttl < 0) {
            Delete delete = new Delete(buildRowKey(key));
            hBaseTemplate.delete(RedisHBaseConfiguration.hbaseTableName(), delete);
            return -2L;
        }
        return pttl;
    }

    /**
     *
     */
    public Long pexpireAt(byte[] key, long millisecondsTimestamp) {
        if (RedisHBaseConfiguration.expireCommandTaskAsyncEnable()) {
            return _pexpireAtAsync(key, millisecondsTimestamp);
        } else {
            try (ICamelliaRedisPipeline pipeline = redisTemplate.pipelined()) {
                List<Put> putList = new ArrayList<>();
                _pexpireAt(pipeline, putList, key, millisecondsTimestamp);
                pipeline.sync();
                if (!putList.isEmpty()) {
                    hBaseTemplate.put(RedisHBaseConfiguration.hbaseTableName(), putList);
                }
            }
            return 1L;
        }
    }

    /**
     *
     */
    public Long expire(byte[] key, int seconds) {
        return pexpireAt(key, System.currentTimeMillis() + seconds * 1000);
    }

    /**
     *
     */
    public Long pexpire(byte[] key, long milliseconds) {
        return pexpireAt(key, System.currentTimeMillis() + milliseconds);
    }

    /**
     *
     */
    public Long expireAt(byte[] key, long unixTime) {
        return pexpireAt(key, unixTime * 1000);
    }

    /**
     *
     */
    public Long exists(byte[]... keys) {
        Long exists = redisTemplate.exists(redisKey(keys));
        if (exists != keys.length) {
            List<byte[]> notExistsKeyList = new ArrayList<>();
            exists = 0L;
            try (ICamelliaRedisPipeline pipelined = redisTemplate.pipelined()) {
                List<Response<Boolean>> list = new ArrayList<>(keys.length);
                for (byte[] key : keys) {
                    Response<Boolean> response = pipelined.exists(redisKey(key));
                    list.add(response);
                }
                pipelined.sync();
                int index = 0;
                for (Response<Boolean> response : list) {
                    if (response.get()) {
                        exists++;
                    } else {
                        byte[] key = keys[index];
                        notExistsKeyList.add(key);
                    }
                    index++;
                }
            }
            if (!notExistsKeyList.isEmpty()) {
                for (byte[] key : notExistsKeyList) {
                    RedisHBaseType redisHBaseType = redisHBaseTypeFromHBase(key);
                    if (redisHBaseType != null) {
                        exists++;
                    }
                }
            }
        }
        return exists;
    }

    //
    private Result getFromHBase(byte[] key) {
        Get get = new Get(buildRowKey(key));
        return hBaseTemplate.get(RedisHBaseConfiguration.hbaseTableName(), get);
    }

    //
    private final LocalCache localCache = new LocalCache(RedisHBaseConfiguration.redisHBaseTypeLocalCacheCapacity());
    private RedisHBaseType redisHBaseType(byte[] key) {
        if (!RedisHBaseConfiguration.redisHBaseTypeLocalCacheEnable()) {
            RedisHBaseType redisHBaseType = RedisHBaseType.byName(redisTemplate.type(redisKey(key)));
            if (redisHBaseType != null) return redisHBaseType;
            return redisHBaseTypeFromHBase(key);
        } else {
            String tag = "type";
            String localCacheKey = Bytes.toHex(key);
            RedisHBaseType redisHBaseType = localCache.get(tag, localCacheKey, RedisHBaseType.class);
            if (redisHBaseType != null) return redisHBaseType;
            long expireMillis = RedisHBaseConfiguration.redisHBaseTypeLocalCacheExpireMillis();
            redisHBaseType = RedisHBaseType.byName(redisTemplate.type(redisKey(key)));
            if (redisHBaseType != null) {
                localCache.put(tag, localCacheKey, redisHBaseType, expireMillis);
                return redisHBaseType;
            }
            redisHBaseType = redisHBaseTypeFromHBase(key);
            if (redisHBaseType != null) {
                localCache.put(tag, localCacheKey, redisHBaseType, expireMillis);
            }
            return redisHBaseType;
        }
    }

    //
    private RedisHBaseType redisHBaseTypeFromHBase(byte[] key) {
        Get get = new Get(buildRowKey(key));
        get.addColumn(CF_D, COL_TYPE);
        get.addColumn(CF_D, COL_EXPIRE_TIME);
        Result result = hBaseTemplate.get(RedisHBaseConfiguration.hbaseTableName(), get);
        byte[] expireTimeRaw = result.getValue(CF_D, COL_EXPIRE_TIME);
        if (expireTimeRaw != null) {
            long expireTime = Bytes.toLong(expireTimeRaw);
            if (expireTime < System.currentTimeMillis()) {
                Delete delete = new Delete(buildRowKey(key));
                hBaseTemplate.delete(RedisHBaseConfiguration.hbaseTableName(), delete);
                return null;
            }
        }
        byte[] typeRaw = result.getValue(CF_D, COL_TYPE);
        if (typeRaw == null) {
            return null;
        }
        for (RedisHBaseType type : RedisHBaseType.values()) {
            if (Bytes.equals(typeRaw, type.raw())) {
                return type;
            }
        }
        return null;
    }

    private final LinkedBlockingQueue<ExpireTask> expireTaskQueue = new LinkedBlockingQueue<>(1000000);
    private Long _pexpireAtAsync(byte[] key, long millisecondsTimestamp) {
        ExpireTask expireTask = new ExpireTask(key, millisecondsTimestamp);
        boolean offer = expireTaskQueue.offer(expireTask);
        if (!offer) {
            logger.warn("expire command discard! key = {}", SafeEncoder.encode(key));
        }
        return 1L;
    }

    private void startAsyncExpireTask() {
        new Thread(() -> {
            List<ExpireTask> buffer = new ArrayList<>();
            while (true) {
                try {
                    if (buffer.isEmpty()) {
                        ExpireTask expireTask = expireTaskQueue.poll(1, TimeUnit.SECONDS);
                        if (expireTask != null) {
                            buffer.add(expireTask);
                        }
                        continue;
                    } else {
                        expireTaskQueue.drainTo(buffer, RedisHBaseConfiguration.expireCommandTaskBatchSize() - 1);
                    }
                    if (buffer.isEmpty()) {
                        continue;
                    }
                    try {
                        if (buffer.size() > 1) {
                            Map<RedisKey, ExpireTask> map = new HashMap<>();
                            for (ExpireTask expireTask : buffer) {
                                RedisKey redisKey = new RedisKey(expireTask.getKey());
                                ExpireTask task = map.get(redisKey);
                                if (task == null) {
                                    map.put(redisKey, expireTask);
                                } else {
                                    if (task.getMillisecondsTimestamp() < expireTask.getMillisecondsTimestamp()) {
                                        map.put(redisKey, expireTask);
                                    }
                                }
                            }
                            buffer.clear();
                            buffer.addAll(map.values());
                        }
                        List<Put> putList = new ArrayList<>();
                        try (ICamelliaRedisPipeline pipeline = redisTemplate.pipelined()) {
                            for (ExpireTask expireTask : buffer) {
                                _pexpireAt(pipeline, putList, expireTask.getKey(), expireTask.getMillisecondsTimestamp());
                            }
                            pipeline.sync();
                        }
                        if (!putList.isEmpty()) {
                            hBaseTemplate.put(RedisHBaseConfiguration.hbaseTableName(), putList);
                        }
                        if (logger.isDebugEnabled()) {
                            logger.debug("flush expire commands, size = {}", buffer.size());
                        }
                    } finally {
                        buffer.clear();
                    }
                } catch (Exception e) {
                    logger.error("expire task error", e);
                }
            }
        }, "expire-command-async-task").start();
    }

    //
    private void _pexpireAt(ICamelliaRedisPipeline pipeline, List<Put> putList, byte[] key, long millisecondsTimestamp) {
        RedisHBaseType redisHBaseType = redisHBaseType(key);
        if (redisHBaseType == null) return;
        long now = System.currentTimeMillis();
        if (redisHBaseType == RedisHBaseType.ZSET) {
            long seconds = (millisecondsTimestamp - now) / 1000;
            if (seconds > RedisHBaseConfiguration.zsetExpireSeconds()) {
                pipeline.expire(redisKey(key), RedisHBaseConfiguration.zsetExpireSeconds());
            } else {
                pipeline.pexpire(redisKey(key), millisecondsTimestamp - now);
            }
        } else {
            pipeline.pexpire(redisKey(key), millisecondsTimestamp - now);
        }
        Put put = new Put(buildRowKey(key));
        put.addColumn(CF_D, COL_EXPIRE_TIME, Bytes.toBytes(millisecondsTimestamp));
        putList.add(put);
    }
}
