package com.netease.nim.camellia.redis.samples;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.util.ResourceTableUtil;
import com.netease.nim.camellia.redis.CamelliaRedisEnv;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import com.netease.nim.camellia.redis.base.resource.RedisResourceUtil;
import com.netease.nim.camellia.redis.jedis.JedisPoolFactory;
import com.netease.nim.camellia.redis.jediscluster.JedisClusterFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Transaction;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by caojiajun on 2023/4/12
 */
public class TestTransactionPipeline {

    public static void main(String[] args) throws InterruptedException {

        String url = "redis://pass123@127.0.0.1:6380";
        ResourceTable resourceTable = ResourceTableUtil.simpleTable(RedisResourceUtil.parseResourceByUrl(new Resource(url)));

        CamelliaRedisEnv redisEnv = new CamelliaRedisEnv.Builder()
                .jedisPoolFactory(new JedisPoolFactory.DefaultJedisPoolFactory(new JedisPoolConfig(), 60*1000*10))
                .jedisClusterFactory(new JedisClusterFactory.DefaultJedisClusterFactory(new JedisPoolConfig(), 60*1000*10, 60*1000*10, 5))
                .build();

        CamelliaRedisTemplate template = new CamelliaRedisTemplate(redisEnv, resourceTable);

        String url1 = "redis-cluster://@127.0.0.1:6379,127.0.0.1:6380";
        String url2 = "redis-cluster://@127.0.0.1:6381,127.0.0.1:6382";
        CamelliaRedisTemplate template1 = new CamelliaRedisTemplate(redisEnv,
                ResourceTableUtil.simpleTable(RedisResourceUtil.parseResourceByUrl(new Resource(url1))));
        CamelliaRedisTemplate template2 = new CamelliaRedisTemplate(redisEnv,
                ResourceTableUtil.simpleTable(RedisResourceUtil.parseResourceByUrl(new Resource(url2))));

        for (int i=0; i<5; i++) {
            new Thread(() -> {
                try {
                    while (true) {
                        String key = UUID.randomUUID().toString();
                        try (Jedis jedis = template.getReadJedis(key)) {
                //                testExec(jedis, key);
                //                testDiscard(jedis, key);
                //                test2(jedis, key);
                            test3(jedis, key);
                        }
                        System.out.println(Thread.currentThread().getName() + ":key=" + key);
                        assertEquals(template1.zrange(key, 0, -1), template2.zrange(key, 0, -1));

                        template.del(key);
                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    public static void testExec(Jedis jedis, String key) {
        assertEquals(Long.valueOf(5L), jedis.incrBy(key, 5L));

        List<Object> expect = new ArrayList<>();
        List<Object> expMulti = new ArrayList<>();

        Pipeline pipe = jedis.pipelined();
        pipe.incrBy(key, 3L);   expect.add(8L);
        pipe.watch(key);        expect.add("OK");
        pipe.multi();           expect.add("OK");
        pipe.incrBy(key, 6L);   expect.add("QUEUED"); expMulti.add(14L);
        pipe.decrBy(key, 2L);   expect.add("QUEUED"); expMulti.add(12);

        pipe.exec();            expect.add(expMulti); // success MULTI
        pipe.incrBy(key, 7L);   expect.add(19L);
        assertEquals(expect, pipe.syncAndReturnAll());
//        jedis.incrBy(key, 7L);   expect.add(19L);
        assertEquals(Long.valueOf(23L), jedis.incrBy(key, 4L));
    }

    public static void testDiscard(Jedis jedis, String key) {
        assertEquals(Long.valueOf(5L), jedis.incrBy(key, 5L));

        List<Object> expect = new ArrayList<>();
//                List<Object> expMulti = new ArrayList<>();

//        assertEquals(jedis.incrBy(key, 3L), Long.valueOf(8L));

        Pipeline pipe = jedis.pipelined();
        pipe.incrBy(key, 3L);   expect.add(8L);
        pipe.watch(key);        expect.add("OK");
        pipe.multi();           expect.add("OK");
        pipe.incrBy(key, 6L);   expect.add("QUEUED");
        pipe.decrBy(key, 2L);   expect.add("QUEUED");

        pipe.discard();            expect.add("OK"); // discard MULTI
        pipe.incrBy(key, 7L);   expect.add(15L);
        assertEquals(expect, pipe.syncAndReturnAll());

//        assertEquals(jedis.incrBy(key, 7L), Long.valueOf(15L));

        assertEquals(Long.valueOf(19L), jedis.incrBy(key, 4L));

    }

    public static void test2(Jedis jedis, String key) {
        List<Object> expMulti = new ArrayList<>();

        assertEquals(Long.valueOf(4L), jedis.incrBy(key, 4L));
        Transaction transaction = jedis.multi();
        transaction.incrBy(key, 4L);   expMulti.add(8L);
        transaction.set(key, "20");      expMulti.add("OK");
        transaction.incrBy(key, 4L);    expMulti.add(24L);

        List<Object> resp = transaction.exec();
        assertEquals(expMulti, resp);

        assertEquals(Long.valueOf(28L), jedis.incrBy(key, 4L));
    }

    public static void test3(Jedis jedis, String key) {
        String field = UUID.randomUUID().toString();
        Map<String, Double> map = new HashMap<>();
        for (int i=0; i<10; i++) {
            map.put(UUID.randomUUID().toString(), ThreadLocalRandom.current().nextDouble(10.0));
        }
        jedis.zadd(key, map);
        jedis.zadd(key, ThreadLocalRandom.current().nextDouble(10.0), field);
        jedis.zrem(key, field);
    }

    private static void assertEquals(Object o1, Object o2) {
        System.out.println(Thread.currentThread().getName() + ":" + o1 + " <--> " + o2);
        if (!String.valueOf(o2).equals(String.valueOf(o1))) {
            System.exit(-1);
        }
    }
}
