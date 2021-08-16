package com.netease.nim.camellia.redis.samples;

import redis.clients.jedis.*;

/**
 * Created by caojiajun on 2021/7/29
 */
public class TestAdaptor {

    public static void main(String[] args) {
//        CamelliaRedisTemplate template = new CamelliaRedisTemplate("redis://@127.0.0.1:6379");
//        JedisPoolCamelliaAdaptor jedisPool = new JedisPoolCamelliaAdaptor(template);
        JedisPool jedisPool = new JedisPool(new JedisPoolConfig(), "127.0.0.1", 6379);
        Jedis jedis = null;
        try {
            //直接请求
            jedis = jedisPool.getResource();
            String setex = jedis.setex("k1", 100, "v1");
            System.out.println(setex);
            String k1 = jedis.get("k1");
            System.out.println(k1);
            //使用pipeline请求
            Pipeline pipelined = jedis.pipelined();
            Response<Long> response1 = pipelined.sadd("sk1", "sv1");
            Response<Long> response2 = pipelined.zadd("zk1", 1.0, "zv1");
            pipelined.sync();
            System.out.println(response1.get());
            System.out.println(response2.get());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }
}
