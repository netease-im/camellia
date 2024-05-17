package com.netease.nim.camellia.redis.proxy.kv.samples;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.core.util.ResourceTableUtil;
import com.netease.nim.camellia.redis.CamelliaRedisEnv;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import com.netease.nim.camellia.redis.jedis.JedisPoolFactory;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Tuple;

import java.util.*;

/**
 * Created by caojiajun on 2024/5/16
 */
public class TestZSetV3 {

    public static void main(String[] args) throws InterruptedException {
        String url = "redis://pass123@127.0.0.1:6381";
//        String url = "redis://@127.0.0.1:6379";
        CamelliaRedisEnv redisEnv = new CamelliaRedisEnv.Builder()
                .jedisPoolFactory(new JedisPoolFactory.DefaultJedisPoolFactory(new JedisPoolConfig(), 6000000))
                .build();
        CamelliaRedisTemplate template = new CamelliaRedisTemplate(redisEnv, ResourceTableUtil.simpleTable(new Resource(url)));

        String key = UUID.randomUUID().toString().replace("-", "");

        template.del(key);

        {
            Map<String, Double> map1 = new HashMap<>();
            map1.put("v1", 1.0);
            map1.put("v2", 2.0);
            map1.put("v3", 3.0);
            map1.put("v4", 4.0);
            map1.put("v5", 5.0);
            map1.put("v6", 6.0);
            Long zadd = template.zadd(key, map1);
            assertEquals(zadd, 6L);
        }
        {
            Long zcard = template.zcard(key);
            assertEquals(zcard, 6L);
        }
        {
            Map<String, Double> map2 = new HashMap<>();
            map2.put("v6", 6.0);
            map2.put("v7", 7.0);
            Long zadd = template.zadd(key, map2);
            assertEquals(zadd, 1L);
        }
        {
            Long zcard = template.zcard(key);
            assertEquals(zcard, 7L);
        }
        {
            Double v1 = template.zscore(key, "v1");
            assertEquals(v1, 1.0);
        }
        {
            Set<String> set = template.zrange(key, 0, -1);
            assertEquals(set.size(), 7);
        }
        {
            Set<String> set = template.zrange(key, 1, 3);
            assertEquals(set.size(), 3);
            Iterator<String> iterator = set.iterator();
            assertEquals(iterator.next(), "v2");
            assertEquals(iterator.next(), "v3");
            assertEquals(iterator.next(), "v4");
        }
        {
            Set<String> set = template.zrevrange(key, -4, -2);
            assertEquals(set.size(), 3);
            Iterator<String> iterator = set.iterator();
            assertEquals(iterator.next(), "v4");
            assertEquals(iterator.next(), "v3");
            assertEquals(iterator.next(), "v2");
        }
        {
            Set<Tuple> tuples = template.zrangeWithScores(key, 2, 5);
            assertEquals(tuples.size(), 4);
            Iterator<Tuple> iterator1 = tuples.iterator();
            assertEquals(iterator1.next().getElement(), "v3");
            assertEquals(iterator1.next().getElement(), "v4");
            assertEquals(iterator1.next().getElement(), "v5");
            assertEquals(iterator1.next().getElement(), "v6");

            Iterator<Tuple> iterator2 = tuples.iterator();
            assertEquals(iterator2.next().getScore(), 3.0);
            assertEquals(iterator2.next().getScore(), 4.0);
            assertEquals(iterator2.next().getScore(), 5.0);
            assertEquals(iterator2.next().getScore(), 6.0);
        }
        {
            Set<Tuple> tuples = template.zrevrangeWithScores(key, 2, 5);
            assertEquals(tuples.size(), 4);
            Iterator<Tuple> iterator1 = tuples.iterator();
            assertEquals(iterator1.next().getElement(), "v5");
            assertEquals(iterator1.next().getElement(), "v4");
            assertEquals(iterator1.next().getElement(), "v3");
            assertEquals(iterator1.next().getElement(), "v2");


            Iterator<Tuple> iterator2 = tuples.iterator();
            assertEquals(iterator2.next().getScore(), 5.0);
            assertEquals(iterator2.next().getScore(), 4.0);
            assertEquals(iterator2.next().getScore(), 3.0);
            assertEquals(iterator2.next().getScore(), 2.0);
        }
        {
            Set<String> set = template.zrangeByScore(key, 1.0, 3.0);
            assertEquals(set.size(), 3);
            Iterator<String> iterator = set.iterator();
            assertEquals(iterator.next(), "v1");
            assertEquals(iterator.next(), "v2");
            assertEquals(iterator.next(), "v3");
        }
        {
            Set<String> set = template.zrevrangeByScore(key, 5.0, 3.0);
            assertEquals(set.size(), 3);
            Iterator<String> iterator = set.iterator();
            assertEquals(iterator.next(), "v5");
            assertEquals(iterator.next(), "v4");
            assertEquals(iterator.next(), "v3");
        }
        {
            Long zremrange = template.zremrangeByRank(key, 1, 2);
            assertEquals(zremrange, 2L);
        }
        {
            Long zremrange = template.zremrangeByScore(key, 3.0, 5.0);
            assertEquals(zremrange, 2L);
        }
        {
            Long zremrange = template.zremrangeByScore(key, 3.0, 5.0);
            assertEquals(zremrange, 0L);
        }
        {
            Long zrem = template.zrem(key, "v1", "v6");
            assertEquals(zrem, 2L);
        }
        {
            Long zrem = template.zrem(key, "v1", "v6");
            assertEquals(zrem, 0L);
        }
        {
            Long zcard = template.zcard(key);
            assertEquals(zcard, 1L);
        }
        template.del(key);
        {
            Map<String, Double> map1 = new HashMap<>();
            map1.put("v1", 1.0);
            map1.put("v2", 2.0);
            map1.put("v3", 3.0);
            map1.put("v4", 4.0);
            map1.put("v5", 5.0);
            map1.put("v6", 6.0);
            Long zadd = template.zadd(key, map1);
            assertEquals(zadd, 6L);
            Set<String> strings = template.zrangeByScore(key, "3.0", "5.0");
            assertEquals(strings.size(), 3);
            Iterator<String> iterator = strings.iterator();
            assertEquals(iterator.next(), "v3");
            assertEquals(iterator.next(), "v4");
            assertEquals(iterator.next(), "v5");

            Set<String> strings1 = template.zrangeByScore(key, "(3.0", "5.0");
            assertEquals(strings1.size(), 2);
            Iterator<String> iterator1 = strings1.iterator();
            assertEquals(iterator1.next(), "v4");
            assertEquals(iterator1.next(), "v5");

            Set<String> strings2 = template.zrangeByScore(key, "(3.0", "+inf");
            assertEquals(strings2.size(), 3);
            Iterator<String> iterator2 = strings2.iterator();
            assertEquals(iterator2.next(), "v4");
            assertEquals(iterator2.next(), "v5");
            assertEquals(iterator2.next(), "v6");
        }

        //end
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
