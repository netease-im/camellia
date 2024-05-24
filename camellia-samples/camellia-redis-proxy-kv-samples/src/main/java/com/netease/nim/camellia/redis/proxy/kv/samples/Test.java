package com.netease.nim.camellia.redis.proxy.kv.samples;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.core.util.ResourceTableUtil;
import com.netease.nim.camellia.redis.CamelliaRedisEnv;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import com.netease.nim.camellia.redis.jedis.JedisPoolFactory;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.EncodeVersion;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyType;
import com.netease.nim.camellia.redis.proxy.upstream.kv.utils.BytesUtils;
import redis.clients.jedis.JedisPoolConfig;

import java.util.Objects;
import java.util.UUID;

/**
 * Created by caojiajun on 2024/4/28
 */
public class Test {

    public static void main(String[] args) throws InterruptedException {
        String url = "redis://pass123@127.0.0.1:6381";
//        String url = "redis://@127.0.0.1:6379";
        CamelliaRedisEnv redisEnv = new CamelliaRedisEnv.Builder()
                .jedisPoolFactory(new JedisPoolFactory.DefaultJedisPoolFactory(new JedisPoolConfig(), 6000000))
                .build();
        CamelliaRedisTemplate template = new CamelliaRedisTemplate(redisEnv, ResourceTableUtil.simpleTable(new Resource(url)));

        for (int i=0; i<4;i++) {
            new Thread(() -> {
                while (true) {
                    String string = UUID.randomUUID().toString();
                    String setex = template.setex(string, 10, string);
                    assertEquals(setex, "OK");
                    String string1 = template.get(string);
                    assertEquals(string, string1);
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }).start();
        }
    }

    private static void assertEquals(Object result, Object expect) {
        if (Objects.equals(result, expect)) {
            System.out.println("SUCCESS");
        } else {
            System.out.println("ERROR, expect " + expect + " but found " + result);
            throw new RuntimeException();
        }
    }
}
