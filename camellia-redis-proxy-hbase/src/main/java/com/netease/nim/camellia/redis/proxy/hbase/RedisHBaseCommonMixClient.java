package com.netease.nim.camellia.redis.proxy.hbase;

import com.netease.nim.camellia.redis.CamelliaRedisTemplate;

import java.util.ArrayList;
import java.util.List;

import static com.netease.nim.camellia.redis.proxy.hbase.util.RedisHBaseUtils.*;

/**
 *
 * Created by caojiajun on 2020/3/5.
 */
public class RedisHBaseCommonMixClient {

    private final CamelliaRedisTemplate redisTemplate;
    private final RedisHBaseZSetMixClient zSetMixClient;
    private final RedisHBaseStringMixClient stringMixClient;
    private final RedisHBaseHashMixClient hashMixClient;

    public RedisHBaseCommonMixClient(CamelliaRedisTemplate redisTemplate,
                                     RedisHBaseZSetMixClient zSetMixClient,
                                     RedisHBaseStringMixClient stringMixClient,
                                     RedisHBaseHashMixClient hashMixClient) {
        this.redisTemplate = redisTemplate;
        this.zSetMixClient = zSetMixClient;
        this.stringMixClient = stringMixClient;
        this.hashMixClient = hashMixClient;
    }

    /**
     *
     */
    public Long del(byte[]... keys) {
        List<byte[]> leaveKeys = new ArrayList<>();
        long ret = 0;
        for (byte[] key : keys) {
            String type = redisTemplate.type(redisKey(key));
            if (type != null) {
                if (type.equalsIgnoreCase("zset")) {
                    Long zremrangeByRank = zSetMixClient.zremrangeByRank(key, 0, -1);
                    if (zremrangeByRank != null && zremrangeByRank > 0) {
                        ret++;
                    }
                } else if (type.equalsIgnoreCase("string")) {
                    Long del = stringMixClient.del(key);
                    if (del > 0) {
                        ret ++;
                    }
                } else if (type.equalsIgnoreCase("hash")) {
                    Long del = hashMixClient.del(key);
                    if (del > 0) {
                        ret ++;
                    }
                } else {
                    leaveKeys.add(redisKey(key));
                }
            }
        }
        if (leaveKeys.isEmpty()) {
            return ret;
        } else {
            return redisTemplate.del(leaveKeys.toArray(new byte[0][0])) + ret;
        }
    }

    /**
     *
     */
    public String type(byte[] key) {
        return redisTemplate.type(redisKey(key));
    }


    /**
     *
     */
    public Long ttl(byte[] key) {
        return redisTemplate.ttl(redisKey(key));
    }

    /**
     *
     */
    public Long pttl(byte[] key) {
        return redisTemplate.pttl(redisKey(key));
    }

    /**
     *
     */
    public Long pexpireAt(byte[] key, long millisecondsTimestamp) {
        return redisTemplate.pexpireAt(redisKey(key), millisecondsTimestamp);
    }

    /**
     *
     */
    public Long expire(byte[] key, int seconds) {
        return redisTemplate.expire(redisKey(key), seconds);
    }

    /**
     *
     */
    public Long pexpire(byte[] key, long milliseconds) {
        return redisTemplate.pexpire(redisKey(key), milliseconds);
    }

    /**
     *
     */
    public Long expireAt(byte[] key, long unixTime) {
        return redisTemplate.expireAt(redisKey(key), unixTime);
    }

    /**
     *
     */
    public Long exists(byte[]... keys) {
        List<byte[]> list = new ArrayList<>(keys.length);
        for (byte[] key : keys) {
            list.add(redisKey(key));
        }
        return redisTemplate.exists(list.toArray(new byte[0][0]));
    }
}
