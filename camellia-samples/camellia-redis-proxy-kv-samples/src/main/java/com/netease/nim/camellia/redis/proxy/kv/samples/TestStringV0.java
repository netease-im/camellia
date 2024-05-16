package com.netease.nim.camellia.redis.proxy.kv.samples;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.core.util.ResourceTableUtil;
import com.netease.nim.camellia.redis.CamelliaRedisEnv;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import com.netease.nim.camellia.redis.jedis.JedisPoolFactory;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.params.SetParams;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Created by caojiajun on 2024/5/16
 */
public class TestStringV0 {
    public static void main(String[] args) throws InterruptedException {
        String url = "redis://pass123@127.0.0.1:6381";
//        String url = "redis://@127.0.0.1:6379";
        CamelliaRedisEnv redisEnv = new CamelliaRedisEnv.Builder()
                .jedisPoolFactory(new JedisPoolFactory.DefaultJedisPoolFactory(new JedisPoolConfig(), 6000000))
                .build();
        CamelliaRedisTemplate template = new CamelliaRedisTemplate(redisEnv, ResourceTableUtil.simpleTable(new Resource(url)));

        String key = UUID.randomUUID().toString().replace("-", "");
        String key1 = UUID.randomUUID().toString().replace("-", "");
        String key2 = UUID.randomUUID().toString().replace("-", "");
        String key3 = UUID.randomUUID().toString().replace("-", "");

        template.del(key);

        {
            String set = template.set(key, "v1");
            assertEquals(set, "OK");
        }
        {
            String set = template.setex(key, 1, "v1");
            assertEquals(set, "OK");
            String string1 = template.get(key);
            assertEquals(string1, "v1");
            Thread.sleep(1050);
            String string2 = template.get(key);
            assertEquals(string2, null);
        }
        {
            String mset = template.mset(key, "v1", key1, "v2", key2, "v3");
            assertEquals(mset, "OK");
            List<String> mget = template.mget(key, key1, key2, key3);
            assertEquals(mget.size(), 4);
            assertEquals(mget.get(0), "v1");
            assertEquals(mget.get(1), "v2");
            assertEquals(mget.get(2), "v3");
            assertEquals(mget.get(3), null);
        }
        template.del(key);
        {
            Long v1 = template.setnx(key, "v1");
            assertEquals(v1, 1L);
            Long v2 = template.setnx(key, "v1");
            assertEquals(v2, 0L);
        }
        template.del(key);
        {
            String set = template.psetex(key, 1000, "v1");
            assertEquals(set, "OK");
            String string1 = template.get(key);
            assertEquals(string1, "v1");
            Thread.sleep(1050);
            String string2 = template.get(key);
            assertEquals(string2, null);
        }
        template.del(key);
        {
            SetParams params = SetParams.setParams().ex(1L).nx();
            String set = template.set(key, "v1", params);
            assertEquals(set, "OK");
            String set1 = template.set(key, "v1", params);
            assertEquals(set1, null);
            String string = template.get(key);
            assertEquals(string, "v1");
            Thread.sleep(1050);
            String string2 = template.get(key);
            assertEquals(string2, null);
        }
        template.del(key);
        {
            SetParams params = SetParams.setParams().px(1000L).nx();
            String set = template.set(key, "v1", params);
            assertEquals(set, "OK");
            String set1 = template.set(key, "v1", params);
            assertEquals(set1, null);
            String string = template.get(key);
            assertEquals(string, "v1");
            Thread.sleep(1050);
            String string2 = template.get(key);
            assertEquals(string2, null);
        }
        template.del(key);
        {
            SetParams params = SetParams.setParams().px(1000L).xx();
            String set = template.set(key, "v1", params);
            assertEquals(set, null);
            String set1 = template.set(key, "v1");
            assertEquals(set1, "OK");
            String set2 = template.set(key, "v1", params);
            assertEquals(set2, "OK");
        }
        template.del(key);
        {
            SetParams params = SetParams.setParams().pxAt(System.currentTimeMillis() + 1000L).xx();
            String set = template.set(key, "v1", params);
            assertEquals(set, null);
            String set1 = template.set(key, "v1");
            assertEquals(set1, "OK");
            String set2 = template.set(key, "v1", params);
            assertEquals(set2, "OK");
        }
        template.del(key);
        {
            SetParams params = SetParams.setParams().exAt(System.currentTimeMillis() + 1L).xx();
            String set = template.set(key, "v1", params);
            assertEquals(set, null);
            String set1 = template.set(key, "v1");
            assertEquals(set1, "OK");
            String set2 = template.set(key, "v1", params);
            assertEquals(set2, "OK");
        }
        template.del(key);
        {
            SetParams params = SetParams.setParams().exAt(System.currentTimeMillis() + 1L).xx().get();
            String set = template.set(key, "v1", params);
            assertEquals(set, null);
            String set1 = template.set(key, "v1");
            assertEquals(set1, "OK");
            String set2 = template.set(key, "v22", params);
            assertEquals(set2, "v1");
        }
        template.del(key);
        {
            SetParams params = SetParams.setParams().keepttl();
            String v1 = template.setex(key, 10,"v1");
            assertEquals(v1, "OK");
            String set = template.set(key, "v2", params);
            assertEquals(set, "OK");
            Long ttl = template.pttl(key);
            assertEquals(ttl > 0, true);
        }


        Thread.sleep(100);
        System.exit(-1);
    }

    private static void assertEquals(Object result, Object expect) throws InterruptedException {
        if (Objects.equals(result, expect)) {
            System.out.println("SUCCESS");
        } else {
            System.out.println("ERROR, expect " + expect + " but found " + result);
            throw new RuntimeException();
        }
    }
}
