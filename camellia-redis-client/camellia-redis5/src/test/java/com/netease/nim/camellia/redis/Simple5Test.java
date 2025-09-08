package com.netease.nim.camellia.redis;

import com.netease.nim.camellia.redis.pipeline.ICamelliaRedisPipeline;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import redis.clients.jedis.Response;

import java.util.UUID;

/**
 * Created by caojiajun on 2025/9/8
 */
public class Simple5Test {

    private CamelliaRedisTemplate template;

    @Before
    public void before() {
//        template = new CamelliaRedisTemplate("redis://@127.0.0.1:6379");
    }

    @Test
    public void test() {
        if (template == null) return;
        String key = UUID.randomUUID().toString();
        String value = UUID.randomUUID().toString();
        String set = template.set(key, value);
        Assert.assertEquals("OK", set);

        String v = template.get(key);
        Assert.assertEquals(value, v);

        try (ICamelliaRedisPipeline pipeline = template.pipelined()) {
            pipeline.setex(key, 10, value + "~");
            Response<String> response = pipeline.get(key);
            pipeline.sync();
            String s = response.get();
            Assert.assertEquals(value + "~", s);
        }
    }
}
