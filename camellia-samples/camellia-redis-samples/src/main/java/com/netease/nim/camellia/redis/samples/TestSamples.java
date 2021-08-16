package com.netease.nim.camellia.redis.samples;

import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import com.netease.nim.camellia.redis.pipeline.ICamelliaRedisPipeline;
import redis.clients.jedis.Response;

/**
 *
 * Created by caojiajun on 2021/7/30
 */
public class TestSamples {

    public static void main(String[] args) {
        CamelliaRedisTemplate template = new CamelliaRedisTemplate("redis://pass@127.0.0.1:6379");
        String k1 = template.get("k1");
        System.out.println(k1);
        String setex = template.setex("k1", 100, "v1");
        System.out.println(setex);

        try (ICamelliaRedisPipeline pipelined = template.pipelined()) {
            Response<Long> response1 = pipelined.sadd("sk1", "sv1");
            Response<Long> response2 = pipelined.zadd("zk1", 1.0, "zv1");
            pipelined.sync();
            System.out.println(response1.get());
            System.out.println(response2.get());
        }
    }
}
