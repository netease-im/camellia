package com.netease.nim.camellia.redis.proxy.kv.samples;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.core.util.ResourceTableUtil;
import com.netease.nim.camellia.redis.CamelliaRedisEnv;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import com.netease.nim.camellia.redis.jedis.JedisPoolFactory;
import redis.clients.jedis.JedisPoolConfig;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by caojiajun on 2024/8/6
 */
public class TestSetV1 {
    private static final ThreadLocal<SimpleDateFormat> dataFormat = ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS"));


    public static void main(String[] args) {
//        String url = "redis://pass123@127.0.0.1:6381";
        String url = "redis://@127.0.0.1:6379";
        CamelliaRedisEnv redisEnv = new CamelliaRedisEnv.Builder()
                .jedisPoolFactory(new JedisPoolFactory.DefaultJedisPoolFactory(new JedisPoolConfig(), 6000000))
                .build();
        CamelliaRedisTemplate template = new CamelliaRedisTemplate(redisEnv, ResourceTableUtil.simpleTable(new Resource(url)));

        testSet(template);

        sleep(100);
        System.exit(-1);
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static final ThreadLocal<String> keyThreadLocal = new ThreadLocal<>();

    public static void testSet(CamelliaRedisTemplate template) {

        try {
            String key = UUID.randomUUID().toString().replace("-", "");
            keyThreadLocal.set(key);
            //
            template.del(key);
            {
                template.sadd(key, "a", "b", "c", "d", "e", "f");

                template.sadd(key, "a", "b", "c", "d", "e", "f", "g");

                template.sadd(key, "a", "b");

                Set<String> smembers = template.smembers(key);
                assertEquals(smembers.size(), 7);
            }
            //
            template.del(key);
            {
                template.sadd(key, "a", "b", "c", "d", "e", "f");

                template.sadd(key, "a", "b", "c", "d", "e", "f", "g");

                Set<String> smembers = template.smembers(key);
                assertEquals(smembers.size(), 7);
                assertEquals(smembers.contains("a"), true);
                assertEquals(smembers.contains("z"), false);

                Boolean a = template.sismember(key, "a");
                assertEquals(a, true);
                Boolean z = template.sismember(key, "z");
                assertEquals(z, false);

                List<Boolean> list = template.smismember(key, "a", "z");
                assertEquals(list.size(), 2);
                assertEquals(list.get(0), true);
                assertEquals(list.get(1), false);

                String srandmember = template.srandmember(key);
                assertEquals(srandmember != null, true);

                List<String> srandmember1 = template.srandmember(key, 2);
                assertEquals(srandmember1.size(), 2);

                Long scard = template.scard(key);
                assertEquals(scard, 7L);

                String spop = template.spop(key);
                assertEquals(spop != null, true);

                Set<String> spop1 = template.spop(key, 2);
                assertEquals(spop1.size(), 2);

                Boolean sismember = template.sismember(key, spop);
                assertEquals(sismember, false);

                for (String s : spop1) {
                    Boolean sismember1 = template.sismember(key, s);
                    assertEquals(sismember1, false);
                }

                Long scard1 = template.scard(key);
                assertEquals(scard1, 4L);

                Set<String> smembers1 = template.smembers(key);

                template.srem(key, smembers1.iterator().next());

                Set<String> smembers2 = template.smembers(key);
                Iterator<String> iterator = smembers2.iterator();
                String next = iterator.next();
                String next1 = iterator.next();
                template.srem(key, next, next1);

                Long scard2 = template.scard(key);
                assertEquals(scard2, 1L);

                Set<String> smembers3 = template.smembers(key);
                template.srem(key, smembers3.toArray(new String[0]));

                String a1 = template.setex(key, 10, "A");
                assertEquals(a1, "OK");

                template.del(key);

                template.sadd(key, "A", "b");
                template.expire(key, 4);
                Set<String> smembers4 = template.smembers(key);
                assertEquals(smembers4.size(), 2);
                Thread.sleep(4050);
                Set<String> smembers5 = template.smembers(key);
                assertEquals(smembers5.size(), 0);
            }

            template.del(key);
        } catch (Exception e) {
            System.out.println("error");
            e.printStackTrace();
            sleep(100);
            System.exit(-1);
        }
    }

    private static void assertEquals(Object result, Object expect) {
        if (Objects.equals(result, expect)) {
            System.out.println("SUCCESS, thread=" + Thread.currentThread().getName()
                    + ", key = " + keyThreadLocal.get() + ", time = " + dataFormat.get().format(new Date()));
        } else {
            System.out.println("ERROR, expect " + expect + " but found " + result + "," +
                    " thread=" + Thread.currentThread().getName() + ", key = " + keyThreadLocal.get() + ", time = " + dataFormat.get().format(new Date()));
            throw new RuntimeException();
        }
    }
}
