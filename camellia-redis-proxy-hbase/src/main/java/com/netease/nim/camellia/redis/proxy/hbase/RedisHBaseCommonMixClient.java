package com.netease.nim.camellia.redis.proxy.hbase;

import com.netease.nim.camellia.hbase.CamelliaHBaseTemplate;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import com.netease.nim.camellia.redis.pipeline.ICamelliaRedisPipeline;
import com.netease.nim.camellia.redis.proxy.hbase.conf.RedisHBaseConfiguration;
import com.netease.nim.camellia.redis.proxy.hbase.model.RedisHBaseType;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import redis.clients.jedis.Response;

import java.util.ArrayList;
import java.util.List;

import static com.netease.nim.camellia.redis.proxy.hbase.util.RedisHBaseUtil.*;

/**
 *
 * Created by caojiajun on 2020/3/5.
 */
public class RedisHBaseCommonMixClient {

    private CamelliaRedisTemplate redisTemplate;
    private RedisHBaseZSetMixClient zSetMixClient;
    private CamelliaHBaseTemplate hBaseTemplate;

    public RedisHBaseCommonMixClient(CamelliaRedisTemplate redisTemplate,
                                     CamelliaHBaseTemplate hBaseTemplate,
                                     RedisHBaseZSetMixClient zSetMixClient) {
        this.redisTemplate = redisTemplate;
        this.zSetMixClient = zSetMixClient;
        this.hBaseTemplate = hBaseTemplate;
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
        RedisHBaseType redisHBaseType = redisHBaseType(key);
        if (redisHBaseType == null) return 0L;
        long now = System.currentTimeMillis();
        if (redisHBaseType == RedisHBaseType.ZSET) {
            long seconds = (millisecondsTimestamp - now) / 1000;
            if (seconds > RedisHBaseConfiguration.zsetExpireSeconds()) {
                redisTemplate.expire(redisKey(key), RedisHBaseConfiguration.zsetExpireSeconds());
            } else {
                redisTemplate.pexpire(redisKey(key), millisecondsTimestamp - now);
            }
        } else {
            redisTemplate.pexpire(redisKey(key), millisecondsTimestamp - now);
        }
        Put put = new Put(buildRowKey(key));
        put.addColumn(CF_D, COL_EXPIRE_TIME, Bytes.toBytes(millisecondsTimestamp));
        hBaseTemplate.put(RedisHBaseConfiguration.hbaseTableName(), put);
        return 1L;
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
    private RedisHBaseType redisHBaseType(byte[] key) {
        RedisHBaseType redisHBaseType = RedisHBaseType.byName(redisTemplate.type(redisKey(key)));
        if (redisHBaseType != null) return redisHBaseType;
        return redisHBaseTypeFromHBase(key);
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
}
