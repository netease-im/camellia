package com.netease.nim.camellia.redis;


import redis.clients.jedis.Jedis;

/**
 * Created by caojiajun on 2022/1/17
 */
public interface CamelliaRedisCommandTask<T> {

    T execute(Jedis jedis);
}
