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
public class SimpleTest {

    private static final boolean enable = Boolean.parseBoolean(System.getProperty("test_enable", "false"));
    private static final String redis_url = System.getProperty("test_redis_url", "redis://@127.0.0.1:6379");
    private static final String redis_cluster_url = System.getProperty("test_redis_cluster_url", "redis-cluster://@10.44.40.24:7101,10.44.40.25:7102,10.44.40.23:7101");
    private static final String redis_cluster_slave_url = System.getProperty("test_redis_cluster_slave_url", "redis-cluster-slaves://@10.44.40.24:7101,10.44.40.25:7102,10.44.40.23:7101");
    private static final String redis_sentinel_url = System.getProperty("test_redis_sentinel_url", "redis-sentinel://@127.0.0.1:26379/mymaster1");
    private static final String redis_sentinel_slaves_url = System.getProperty("test_redis_sentinel_slaves_url", "redis-sentinel-slaves://@127.0.0.1:26379/mymaster1");

    @Test
    public void autoTest() {
        try {
            if (!enable) {
                return;
            }
            test(redis());
            test(redisCluster());
            test(shardRedis());
            test(readWriteSeparate1());
            test(readWriteSeparate2());
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
        if (redis_cluster_url.isEmpty() || redis_url.isEmpty() || redis_sentinel_url.isEmpty()) {
            return null;
        }
        String s = "{\n" +
                "  \"type\": \"sharding\",\n" +
                "  \"operation\": {\n" +
                "    \"operationMap\": {\n" +
                "      \"0-1\": \"" + redis_url + "\",\n" +
                "      \"2-3\": \"" + redis_cluster_url + "\",\n" +
                "      \"4-5\": \"" + redis_sentinel_url + "\"\n" +
                "    },\n" +
                "    \"bucketSize\": 6\n" +
                "  }\n" +
                "}\n";
        ResourceTable resourceTable = ReadableResourceTableUtil.parseTable(s);
        return new CamelliaRedisTemplate(resourceTable);
    }

    private CamelliaRedisTemplate readWriteSeparate1() {
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

    private CamelliaRedisTemplate readWriteSeparate2() {
        if (redis_sentinel_url.isEmpty() || redis_sentinel_slaves_url.isEmpty()) {
            return null;
        }
        String s = "{\n" +
                "  \"type\": \"simple\",\n" +
                "  \"operation\": {\n" +
                "    \"read\": \"" + redis_sentinel_slaves_url + "\",\n" +
                "    \"type\": \"rw_separate\",\n" +
                "    \"write\": \"" + redis_sentinel_url + "\"\n" +
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
        String k3 = UUID.randomUUID().toString();
        String hashField = UUID.randomUUID().toString();
        String hashValue = UUID.randomUUID().toString();
        String k4 = UUID.randomUUID().toString();
        String setMember = UUID.randomUUID().toString();

        try (ICamelliaRedisPipeline pipeline = template.pipelined()) {
            String[] array = kv.keySet().toArray(new String[0]);
            Response<Long> zadd = pipeline.zadd(k2, score, member);
            pipeline.hset(k3, hashField, hashValue);
            pipeline.sadd(k4, setMember);
            pipeline.sync();
            sleep(1000);
            Response<Set<String>> zrange = pipeline.zrange(k2, 0, -1);
            Response<List<String>> mget = pipeline.mget(array);
            Response<String> hget = pipeline.hget(k3, hashField);
            Response<Set<String>> smembers = pipeline.smembers(k4);
            pipeline.sync();
            List<String> strings = mget.get();
            for (int i=0; i<array.length; i++) {
                Assert.assertEquals(kv.get(array[i]), strings.get(i));
            }
            Assert.assertEquals(1L, zadd.get().longValue());
            Assert.assertEquals(1L, zrange.get().size());
            Assert.assertEquals(member, zrange.get().iterator().next());
            Assert.assertEquals(hashValue, hget.get());
            Assert.assertEquals(setMember, smembers.get().iterator().next());
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
