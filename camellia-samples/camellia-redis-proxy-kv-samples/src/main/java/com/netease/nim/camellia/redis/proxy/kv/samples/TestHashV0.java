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
 * Created by caojiajun on 2024/4/12
 */
public class TestHashV0 {

    private static final ThreadLocal<SimpleDateFormat> dataFormat = ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS"));


    public static void main(String[] args) {
        String url = "redis://pass123@127.0.0.1:6381";
//        String url = "redis://@127.0.0.1:6379";
        CamelliaRedisEnv redisEnv = new CamelliaRedisEnv.Builder()
                .jedisPoolFactory(new JedisPoolFactory.DefaultJedisPoolFactory(new JedisPoolConfig(), 6000000))
                .build();
        CamelliaRedisTemplate template = new CamelliaRedisTemplate(redisEnv, ResourceTableUtil.simpleTable(new Resource(url)));

        testHash(template);

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

    public static void testHash(CamelliaRedisTemplate template) {

        try {
            String key = UUID.randomUUID().toString().replace("-", "");
            keyThreadLocal.set(key);
            //
            template.del(key);
            {
                Long hset1 = template.hset(key, "f1", "v1");
                assertEquals(hset1, 1L);

                Long hset2 = template.hset(key, "f1", "v1");
                assertEquals(hset2, 0L);

                Long hset3 = template.hset(key, "f1", "v2");
                assertEquals(hset3, 0L);

                Map<String, String> stringStringMap = template.hgetAll(key);
                assertEquals(stringStringMap.size(), 1);
            }
            template.del(key);
            {
                Long hset1 = template.hset(key, "f1", "v1");
                assertEquals(hset1, 1L);

                Long hset2 = template.hset(key, "f1", "v1");
                assertEquals(hset2, 0L);

                Long hlen1 = template.hlen(key);
                assertEquals(hlen1, 1L);

                Long hdel1 = template.hdel(key, "f1");
                assertEquals(hdel1, 1L);

                Long hdel2 = template.hdel(key, "f1");
                assertEquals(hdel2, 0L);

                Long hlen2 = template.hlen(key);
                assertEquals(hlen2, 0L);
            }

            //
            template.del(key);
            {
                Long hset1 = template.hset(key, "f1", "v1");
                assertEquals(hset1, 1L);

                Long hset2 = template.hset(key, "f2", "v2");
                assertEquals(hset2, 1L);

                String hget1 = template.hget(key, "f1");
                assertEquals(hget1, "v1");

                String hget2 = template.hget(key, "f1");
                assertEquals(hget2, "v1");

                Long hlen1 = template.hlen(key);
                assertEquals(hlen1, 2L);

                Long hdel1 = template.hdel(key, "f1");
                assertEquals(hdel1, 1L);

                String hget3 = template.hget(key, "f1");
                assertEquals(hget3, null);

                Long hlen2 = template.hlen(key);
                assertEquals(hlen2, 1L);
            }

            //
            template.del(key);
            {
                Long hset1 = template.hset(key, "f1", "v1");
                assertEquals(hset1, 1L);

                Long hset2 = template.hset(key, "f2", "v2");
                assertEquals(hset2, 1L);

                String hget1 = template.hget(key, "f1");
                assertEquals(hget1, "v1");

                Map<String, String> hgetall1 = template.hgetAll(key);
                assertEquals(hgetall1.size(), 2);
                assertEquals(hgetall1.get("f1"), "v1");
                assertEquals(hgetall1.get("f2"), "v2");

                Long hset3 = template.hset(key, "f1", "v11");
                assertEquals(hset3, 0L);

                String hget3 = template.hget(key, "f1");
                assertEquals(hget3, "v11");

                Map<String, String> hgetall2 = template.hgetAll(key);
                assertEquals(hgetall2.size(), 2);
                assertEquals(hgetall2.get("f1"), "v11");
                assertEquals(hgetall2.get("f2"), "v2");

                Long hdel1 = template.hdel(key, "f1");
                assertEquals(hdel1, 1L);

                String hget4 = template.hget(key, "f1");
                assertEquals(hget4, null);

                Map<String, String> hgetall3 = template.hgetAll(key);
                assertEquals(hgetall3.size(), 1);
                assertEquals(hgetall3.get("f2"), "v2");

                Map<String, String> map = new HashMap<>();
                map.put("f1", "v1");
                map.put("f2", "v2");
                map.put("f3", "v3");
                map.put("f4", "v4");
                String hmset1 = template.hmset(key, map);
                assertEquals(hmset1, "OK");

                String hget5 = template.hget(key, "f1");
                assertEquals(hget5, "v1");

                String hget6 = template.hget(key, "f2");
                assertEquals(hget6, "v2");

                String hget7 = template.hget(key, "f3");
                assertEquals(hget7, "v3");

                String hget8 = template.hget(key, "f4");
                assertEquals(hget8, "v4");

                String hget9 = template.hget(key, "f5");
                assertEquals(hget9, null);

                Map<String, String> hgetall4 = template.hgetAll(key);
                assertEquals(hgetall4.size(), 4);
                assertEquals(hgetall4.get("f1"), "v1");
                assertEquals(hgetall4.get("f2"), "v2");
                assertEquals(hgetall4.get("f3"), "v3");
                assertEquals(hgetall4.get("f4"), "v4");

                Long hlen2 = template.hlen(key);
                assertEquals(hlen2, 4L);

                Long expire1 = template.expire(key, 5);
                assertEquals(expire1, 1L);

                String hget10 = template.hget(key, "f1");
                assertEquals(hget10, "v1");

                Map<String, String> hgetall5 = template.hgetAll(key);
                assertEquals(hgetall5.size(), 4);

                Thread.sleep(5050);

                String hget11 = template.hget(key, "f1");
                assertEquals(hget11, null);

                Map<String, String> hgetall6 = template.hgetAll(key);
                assertEquals(hgetall6.size(), 0);
            }
            template.del(key);
            {
                Long hset1 = template.hset(key, "f1", "v1");
                assertEquals(hset1, 1L);

                Long hset2 = template.hset(key, "f2", "v2");
                assertEquals(hset2, 1L);

                Set<String> hkeys = template.hkeys(key);
                assertEquals(hkeys.size(), 2);
                assertEquals(hkeys.contains("f1"), true);
                assertEquals(hkeys.contains("f2"), true);

                List<String> hvals = template.hvals(key);
                assertEquals(hvals.size(), 2);
                assertEquals(hvals.contains("v1"), true);
                assertEquals(hvals.contains("v1"), true);
            }

            template.del(key);
            {
                Long hset1 = template.hset(key, "f1", "v1");
                assertEquals(hset1, 1L);

                Long hset2 = template.hset(key, "f2", "v2");
                assertEquals(hset2, 1L);

                Boolean f1 = template.hexists(key, "f1");
                assertEquals(f1, true);

                Boolean f3 = template.hexists(key, "f3");
                assertEquals(f3, false);

                template.hdel(key, "f1");

                Boolean f11 = template.hexists(key, "f1");
                assertEquals(f11, false);

                template.del(key);

                Boolean f2 = template.hexists(key, "f2");
                assertEquals(f2, false);
            }

            template.del(key);
            {

                Long hsetnx = template.hsetnx(key, "f1", "v1");
                assertEquals(hsetnx, 1L);

                Long hsetnx1 = template.hsetnx(key, "f1", "v1");
                assertEquals(hsetnx1, 0L);

                Long hset1 = template.hset(key, "f1", "v1");
                assertEquals(hset1, 0L);

                Long hset2 = template.hset(key, "f2", "v2");
                assertEquals(hset2, 1L);

                Long hlen = template.hlen(key);
                assertEquals(hlen, 2L);

                Long hdel = template.hdel(key, "f1");
                assertEquals(hdel, 1L);

                Long hsetnx2 = template.hsetnx(key, "f1", "v1");
                assertEquals(hsetnx2, 1L);
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
