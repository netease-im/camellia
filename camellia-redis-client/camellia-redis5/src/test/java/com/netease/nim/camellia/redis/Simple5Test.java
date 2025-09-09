package com.netease.nim.camellia.redis;

import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.util.ReadableResourceTableUtil;
import com.netease.nim.camellia.redis.pipeline.ICamelliaRedisPipeline;
import org.junit.Assert;
import org.junit.Test;
import redis.clients.jedis.Response;

import java.util.*;

/**
 * Created by caojiajun on 2025/9/8
 */
public class Simple5Test {

    private static final String redis_url = System.getProperty("test_redis_url", "");
    private static final String redis_cluster_url = System.getProperty("test_redis_cluster_url", "");
    private static final String redis_cluster_slave_url = System.getProperty("test_redis_cluster_slave_url", "");

    @Test
    public void autoTest() {
        try {
            test(redis());
            test(redisCluster());
            test(shardRedis());
            test(readWriteSeparate());
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private CamelliaRedisTemplate redis() {
        if (redis_url.isEmpty()) {
            return null;
        }
        return new CamelliaRedisTemplate(redis_url);
    }

    private CamelliaRedisTemplate redisCluster() {
        if (redis_cluster_url.isEmpty()) {
            return null;
        }
        return new CamelliaRedisTemplate(redis_cluster_url);
    }

    private CamelliaRedisTemplate shardRedis() {
        if (redis_cluster_url.isEmpty() || redis_url.isEmpty()) {
            return null;
        }
        String s = "{\n" +
                "  \"type\": \"sharding\",\n" +
                "  \"operation\": {\n" +
                "    \"operationMap\": {\n" +
                "      \"0-2-4\": \"" + redis_url + "\",\n" +
                "      \"1-3-5\": \"" + redis_cluster_url + "\"\n" +
                "    },\n" +
                "    \"bucketSize\": 6\n" +
                "  }\n" +
                "}\n";
        ResourceTable resourceTable = ReadableResourceTableUtil.parseTable(s);
        return new CamelliaRedisTemplate(resourceTable);
    }

    private CamelliaRedisTemplate readWriteSeparate() {
        if (redis_cluster_url.isEmpty() || redis_cluster_slave_url.isEmpty()) {
            return null;
        }
        String s = "{\n" +
                "  \"type\": \"simple\",\n" +
                "  \"operation\": {\n" +
                "    \"read\": \"" + redis_cluster_slave_url + "\",\n" +
                "    \"type\": \"rw_separate\",\n" +
                "    \"write\": \"" + redis_cluster_url + "\"\n" +
                "  }\n" +
                "}";
        ResourceTable resourceTable = ReadableResourceTableUtil.parseTable(s);
        return new CamelliaRedisTemplate(resourceTable);
    }

    public void test(CamelliaRedisTemplate template) {
        if (template == null) return;

        String key = UUID.randomUUID().toString();
        String value = UUID.randomUUID().toString();
        String set = template.set(key, value);
        Assert.assertEquals("OK", set);

        String v = template.get(key);
        Assert.assertEquals(value, v);

        Map<String, String> kv = new HashMap<>();
        for (int i=0; i<10; i++) {
            kv.put(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        }
        {
            String[] args = new String[kv.size() * 2];
            int i = 0;
            for (Map.Entry<String, String> entry : kv.entrySet()) {
                args[i] = entry.getKey();
                args[i + 1] = entry.getValue();
                i += 2;
            }
            template.mset(args);
        }

        {
            String[] array = kv.keySet().toArray(new String[0]);
            List<String> strings = template.mget(array);
            for (int i=0; i<array.length; i++) {
                Assert.assertEquals(kv.get(array[i]), strings.get(i));
            }
        }

        try (ICamelliaRedisPipeline pipeline = template.pipelined()) {
            pipeline.setex(key, 10, value + "~");
            pipeline.sync();
            sleep(1000);
            Response<String> response = pipeline.get(key);
            pipeline.sync();
            String s = response.get();
            Assert.assertEquals(value + "~", s);
        }

        String k2 = UUID.randomUUID().toString();
        String member = UUID.randomUUID().toString();
        double score = System.currentTimeMillis();

        try (ICamelliaRedisPipeline pipeline = template.pipelined()) {
            String[] array = kv.keySet().toArray(new String[0]);
            Response<Long> zadd = pipeline.zadd(k2, score, member);
            pipeline.sync();
            sleep(1000);
            Response<List<String>> zrange = pipeline.zrange(k2, 0, -1);
            Response<List<String>> mget = pipeline.mget(array);
            pipeline.sync();
            List<String> strings = mget.get();
            for (int i=0; i<array.length; i++) {
                Assert.assertEquals(kv.get(array[i]), strings.get(i));
            }
            Assert.assertEquals(1L, zadd.get().longValue());
            Assert.assertEquals(1L, zrange.get().size());
            Assert.assertEquals(member, zrange.get().iterator().next());
        }
        System.out.println("test success");
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
