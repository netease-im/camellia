package com.netease.nim.camellia.redis.proxy.kv.samples;

import com.netease.nim.camellia.core.client.env.MultiWriteType;
import com.netease.nim.camellia.core.client.env.ProxyEnv;
import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.util.ResourceTableUtil;
import com.netease.nim.camellia.redis.CamelliaRedisEnv;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import com.netease.nim.camellia.redis.pipeline.ICamelliaRedisPipeline;
import redis.clients.jedis.Response;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Created by caojiajun on 2024/8/1
 */
public class TestMultiWrite {

    public static void main(String[] args) throws Exception {
        String url1 = "redis://@127.0.0.1:6379";
        String url2 = "redis://@127.0.0.1:6375";

        test(MultiWriteType.SINGLE_THREAD, url1, url2);
        test(MultiWriteType.MULTI_THREAD_CONCURRENT, url1, url2);
        test(MultiWriteType.ASYNC_MULTI_THREAD, url1, url2);
        test(MultiWriteType.MISC_ASYNC_MULTI_THREAD, url1, url2);

        Thread.sleep(100);
        System.exit(-1);
    }

    private static void test(MultiWriteType multiWriteType, String url1, String url2) {
        ResourceTable resourceTable = ResourceTableUtil.simple2W1RTable(new Resource(url1), new Resource(url1), new Resource(url2));

        ProxyEnv proxyEnv = new ProxyEnv.Builder()
                .multiWriteType(multiWriteType)
                .build();
        CamelliaRedisEnv redisEnv = new CamelliaRedisEnv.Builder()
                .proxyEnv(proxyEnv)
                .build();
        CamelliaRedisTemplate template = new CamelliaRedisTemplate(redisEnv, resourceTable);
        CamelliaRedisTemplate template1 = new CamelliaRedisTemplate(url1);
        CamelliaRedisTemplate template2 = new CamelliaRedisTemplate(url2);

        {
            String key = UUID.randomUUID().toString();
            template.del(key);
            String value = UUID.randomUUID().toString();
            String k1 = template.setex(key, 1000, value);
            assertEquals(k1, "OK");

            String s = template.get(key);
            assertEquals(s, value);

            String s1 = template1.get(key);
            assertEquals(s1, value);

            String s2 = template2.get(key);
            assertEquals(s2, value);
        }

        {
            String key1 = UUID.randomUUID().toString();
            String key2 = UUID.randomUUID().toString();
            String key3 = UUID.randomUUID().toString();
            String key4 = UUID.randomUUID().toString();
            template.del(key1, key2, key3, key4);

            String value1 = UUID.randomUUID().toString();
            String value2 = UUID.randomUUID().toString();
            String value3 = UUID.randomUUID().toString();

            template.mset(key1, value1, key2, value2, key3, value3);

            {
                List<String> mget = template.mget(key1, key2, key3, key4);
                assertEquals(mget.size(), 4);
                assertEquals(mget.get(0), value1);
                assertEquals(mget.get(1), value2);
                assertEquals(mget.get(2), value3);
                assertEquals(mget.get(3), null);

                List<String> mget1 = template1.mget(key1, key2, key3, key4);
                assertEquals(mget1.size(), 4);
                assertEquals(mget1.get(0), value1);
                assertEquals(mget1.get(1), value2);
                assertEquals(mget1.get(2), value3);
                assertEquals(mget1.get(3), null);

                List<String> mget2 = template2.mget(key1, key2, key3, key4);
                assertEquals(mget2.size(), 4);
                assertEquals(mget2.get(0), value1);
                assertEquals(mget2.get(1), value2);
                assertEquals(mget2.get(2), value3);
                assertEquals(mget2.get(3), null);
            }

            template.del(key2, key3);

            {
                List<String> mget = template.mget(key1, key2, key3, key4);
                assertEquals(mget.size(), 4);
                assertEquals(mget.get(0), value1);
                assertEquals(mget.get(1), null);
                assertEquals(mget.get(2), null);
                assertEquals(mget.get(3), null);

                List<String> mget1 = template1.mget(key1, key2, key3, key4);
                assertEquals(mget1.size(), 4);
                assertEquals(mget1.get(0), value1);
                assertEquals(mget1.get(1), null);
                assertEquals(mget1.get(2), null);
                assertEquals(mget1.get(3), null);

                List<String> mget2 = template2.mget(key1, key2, key3, key4);
                assertEquals(mget2.size(), 4);
                assertEquals(mget2.get(0), value1);
                assertEquals(mget2.get(1), null);
                assertEquals(mget2.get(2), null);
                assertEquals(mget2.get(3), null);
            }
        }


        {
            try (ICamelliaRedisPipeline pipelined = template.pipelined()) {
                String key1 = UUID.randomUUID().toString();
                String key2 = UUID.randomUUID().toString();
                String key3 = UUID.randomUUID().toString();
                pipelined.del(key1);
                pipelined.del(key2);
                pipelined.del(key3);
                String value1 = UUID.randomUUID().toString();
                String value2 = UUID.randomUUID().toString();
                String value3 = UUID.randomUUID().toString();
                Response<String> setex1 = pipelined.setex(key1, 1000, value1);
                Response<String> setex2 = pipelined.setex(key2, 1000, value2);
                Response<String> setex3 = pipelined.setex(key3, 1000, value3);
                pipelined.sync();
                assertEquals(setex1.get(), "OK");
                assertEquals(setex2.get(), "OK");
                assertEquals(setex3.get(), "OK");

                assertEquals(template1.get(key1), value1);
                assertEquals(template1.get(key2), value2);
                assertEquals(template1.get(key3), value3);

                assertEquals(template2.get(key1), value1);
                assertEquals(template2.get(key2), value2);
                assertEquals(template2.get(key3), value3);

                Response<Long> del1 = pipelined.del(key1);
                Response<Long> del2 = pipelined.del(key2);
                Response<Long> del3 = pipelined.del(key3);
                pipelined.sync();
                assertEquals(del1.get(), 1L);
                assertEquals(del2.get(), 1L);
                assertEquals(del3.get(), 1L);

                assertEquals(template1.get(key1), null);
                assertEquals(template1.get(key2), null);
                assertEquals(template1.get(key3), null);

                assertEquals(template2.get(key1), null);
                assertEquals(template2.get(key2), null);
                assertEquals(template2.get(key3), null);
            }
        }

        {
            String key = UUID.randomUUID().toString();
            String value = UUID.randomUUID().toString();
            template.eval("return redis.call('set', KEYS[1], ARGV[1])", 1, key, value);

            assertEquals(template.get(key), value);
            assertEquals(template1.get(key), value);
            assertEquals(template2.get(key), value);
        }
    }

    private static void assertEquals(Object result, Object expect) {
        if (Objects.equals(result, expect)) {
            System.out.println("SUCCESS, thread=" + Thread.currentThread().getName());
        } else {
            System.out.println("ERROR, expect " + expect + " but found " + result + "," +
                    " thread=" + Thread.currentThread().getName());
            throw new RuntimeException();
        }
    }
}
