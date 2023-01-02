package com.netease.nim.camellia.spring.redis.base;

import org.springframework.data.redis.connection.jedis.JedisConnection;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 *
 * Created by caojiajun on 2021/7/30
 */
public class CamelliaRedisTemplateJedisConnection extends JedisConnection {

    public CamelliaRedisTemplateJedisConnection(Jedis jedis) {
        super(jedis);
    }

    public CamelliaRedisTemplateJedisConnection(Jedis jedis, JedisPool pool) {
        super(jedis, pool, 0);
    }

    public CamelliaRedisTemplateJedisConnection(Jedis jedis, JedisPool pool, String clientName) {
        super(jedis, pool, 0, clientName);
    }
}
