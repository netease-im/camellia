package com.netease.nim.camellia.redis.proxy.discovery.jedis;

import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * Created by caojiajun on 2019/11/13.
 */
public class RedisProxyJedisPoolContext {

    private static final ConcurrentHashMap<Long, RedisProxyJedisPool> poolMap = new ConcurrentHashMap<>();

    public static void init(RedisProxyJedisPool pool) {
        poolMap.put(pool.getId(), pool);
    }

    public static RedisProxyJedisPool get(long id) {
        return poolMap.get(id);
    }

    public static void remove(RedisProxyJedisPool pool) {
        poolMap.remove(pool.getId());
    }
}
