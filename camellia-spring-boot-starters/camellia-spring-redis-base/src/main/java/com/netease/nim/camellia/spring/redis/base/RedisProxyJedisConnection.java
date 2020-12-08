package com.netease.nim.camellia.spring.redis.base;

import org.springframework.data.redis.connection.jedis.JedisConnection;
import redis.clients.jedis.Jedis;
import redis.clients.util.Pool;

/**
 *
 * Created by caojiajun on 2020/12/2
 */
public class RedisProxyJedisConnection extends JedisConnection {
    public RedisProxyJedisConnection(Jedis jedis) {
        super(jedis);
    }

    public RedisProxyJedisConnection(Jedis jedis, Pool<Jedis> pool) {
        super(jedis, pool, 0);
    }

    public RedisProxyJedisConnection(Jedis jedis, Pool<Jedis> pool, String clientName) {
        super(jedis, pool, 0, clientName);
    }
}
