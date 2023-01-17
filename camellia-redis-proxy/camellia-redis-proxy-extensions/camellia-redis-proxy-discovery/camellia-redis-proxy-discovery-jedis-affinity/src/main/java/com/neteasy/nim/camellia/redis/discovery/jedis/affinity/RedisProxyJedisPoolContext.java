package com.neteasy.nim.camellia.redis.discovery.jedis.affinity;

import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * Created by caojiajun on 2019/11/13.
 */
public class RedisProxyJedisPoolContext {

    private static final ConcurrentHashMap<Long, RedisProxyAffinityJedisPool> poolMap = new ConcurrentHashMap<>();

    public static void init(RedisProxyAffinityJedisPool pool) {
        poolMap.put(pool.getId(), pool);
    }

    public static RedisProxyAffinityJedisPool get(long id) {
        return poolMap.get(id);
    }

    public static void remove(RedisProxyAffinityJedisPool pool) {
        poolMap.remove(pool.getId());
    }
}
