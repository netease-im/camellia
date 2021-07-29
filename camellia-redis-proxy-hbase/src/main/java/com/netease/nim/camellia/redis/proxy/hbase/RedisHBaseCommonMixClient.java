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
                } else if (type.equalsIgnoreCase("string") || type.equalsIgnoreCase("none")) {
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
        String type = type(key);
        if (type.equalsIgnoreCase("string") || type.equalsIgnoreCase("none")) {
            Long pttl = stringMixClient.pttl(key);
            return pttl < 0 ? pttl : pttl / 1000L;
        }
        return redisTemplate.ttl(redisKey(key));
    }

    /**
     *
     */
    public Long pttl(byte[] key) {
        String type = type(key);
        if (type.equalsIgnoreCase("string") || type.equalsIgnoreCase("none")) {
            return stringMixClient.pttl(key);
        }
        return redisTemplate.pttl(redisKey(key));
    }

    /**
     *
     */
    public Long pexpireAt(byte[] key, long millisecondsTimestamp) {
        String type = type(key);
        if (type.equalsIgnoreCase("string") || type.equalsIgnoreCase("none")) {
            return stringMixClient.pexpireAt(key, millisecondsTimestamp);
        }
        return redisTemplate.pexpireAt(redisKey(key), millisecondsTimestamp);
    }

    /**
     *
     */
    public Long expire(byte[] key, int seconds) {
        String type = type(key);
        if (type.equals("string") || type.equals("none")) {
            return stringMixClient.pexpireAt(key, System.currentTimeMillis() + seconds * 1000L);
        }
        return redisTemplate.expire(redisKey(key), seconds);
    }

    /**
     *
     */
    public Long pexpire(byte[] key, long milliseconds) {
        String type = type(key);
        if (type.equalsIgnoreCase("string") || type.equalsIgnoreCase("none")) {
            return stringMixClient.pexpireAt(key, System.currentTimeMillis() + milliseconds);
        }
        return redisTemplate.pexpire(redisKey(key), milliseconds);
    }

    /**
     *
     */
    public Long expireAt(byte[] key, long unixTime) {
        String type = type(key);
        if (type.equalsIgnoreCase("string") || type.equalsIgnoreCase("none")) {
            return stringMixClient.pexpireAt(key, unixTime * 1000L);
        }
        return redisTemplate.expireAt(redisKey(key), unixTime);
    }

    /**
     *
     */
    public Long exists(byte[]... keys) {
        Long ret = 0L;
        for (byte[] key : keys) {
            String type = type(key);
            if (type.equalsIgnoreCase("string") || type.equalsIgnoreCase("none")) {
                ret += stringMixClient.exists(key);
            } else {
                ret += 1;
            }
        }
        return ret;
    }
}
