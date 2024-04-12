package com.netease.nim.camellia.redis.proxy.kv.samples;

import com.netease.nim.camellia.redis.CamelliaRedisTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Created by caojiajun on 2024/4/12
 */
public class TestHashV0 {

    public static void main(String[] args) throws InterruptedException {
        String url = "redis://pass123@127.0.0.1:6381";
        CamelliaRedisTemplate template = new CamelliaRedisTemplate(url);

        //
        template.del("key1");
        {
            Long hset1 = template.hset("key1", "f1", "v1");
            assertEquals(hset1, 1L);

            Long hset2 = template.hset("key1", "f1", "v1");
            assertEquals(hset2, 0L);

            Long hlen1 = template.hlen("key1");
            assertEquals(hlen1, 1L);

            Long hdel1 = template.hdel("key1", "f1");
            assertEquals(hdel1, 1L);

            Long hdel2 = template.hdel("key1", "f1");
            assertEquals(hdel2, 0L);

            Long hlen2 = template.hlen("key1");
            assertEquals(hlen2, 0L);
        }

        //
        template.del("key1");
        {
            Long hset1 = template.hset("key1", "f1", "v1");
            assertEquals(hset1, 1L);

            Long hset2 = template.hset("key1", "f2", "v2");
            assertEquals(hset2, 1L);

            String hget1 = template.hget("key1", "f1");
            assertEquals(hget1, "v1");

            String hget2 = template.hget("key1", "f1");
            assertEquals(hget2, "v1");

            Long hlen1 = template.hlen("key1");
            assertEquals(hlen1, 2L);

            Long hdel1 = template.hdel("key1", "f1");
            assertEquals(hdel1, 1L);

            String hget3 = template.hget("key1", "f1");
            assertEquals(hget3, null);

            Long hlen2 = template.hlen("key1");
            assertEquals(hlen2, 1L);
        }

        //
        template.del("key1");
        {
            Long hset1 = template.hset("key1", "f1", "v1");
            assertEquals(hset1, 1L);

            Long hset2 = template.hset("key1", "f2", "v2");
            assertEquals(hset2, 1L);

            String hget1 = template.hget("key1", "f1");
            assertEquals(hget1, "v1");

            Map<String, String> hgetall1 = template.hgetAll("key1");
            assertEquals(hgetall1.size(), 2);
            assertEquals(hgetall1.get("f1"), "v1");
            assertEquals(hgetall1.get("f2"), "v2");

            Long hset3 = template.hset("key1", "f1", "v11");
            assertEquals(hset3, 0L);

            String hget3 = template.hget("key1", "f1");
            assertEquals(hget3, "v11");

            Map<String, String> hgetall2 = template.hgetAll("key1");
            assertEquals(hgetall2.size(), 2);
            assertEquals(hgetall2.get("f1"), "v11");
            assertEquals(hgetall2.get("f2"), "v2");

            Long hdel1 = template.hdel("key1", "f1");
            assertEquals(hdel1, 1L);

            String hget4 = template.hget("key1", "f1");
            assertEquals(hget4, null);

            Map<String, String> hgetall3 = template.hgetAll("key1");
            assertEquals(hgetall3.size(), 1);
            assertEquals(hgetall3.get("f2"), "v2");

            Map<String, String> map = new HashMap<>();
            map.put("f1", "v1");
            map.put("f2", "v2");
            map.put("f3", "v3");
            map.put("f4", "v4");
            String hmset1 = template.hmset("key1", map);
            assertEquals(hmset1, "OK");

            String hget5 = template.hget("key1", "f1");
            assertEquals(hget5, "v1");

            String hget6 = template.hget("key1", "f2");
            assertEquals(hget6, "v2");

            String hget7 = template.hget("key1", "f3");
            assertEquals(hget7, "v3");

            String hget8 = template.hget("key1", "f4");
            assertEquals(hget8, "v4");

            String hget9 = template.hget("key1", "f5");
            assertEquals(hget9, null);

            Map<String, String> hgetall4 = template.hgetAll("key1");
            assertEquals(hgetall4.size(), 4);
            assertEquals(hgetall4.get("f1"), "v1");
            assertEquals(hgetall4.get("f2"), "v2");
            assertEquals(hgetall4.get("f3"), "v3");
            assertEquals(hgetall4.get("f4"), "v4");

            Long hlen2 = template.hlen("key1");
            assertEquals(hlen2, 4L);

            Long expire1 = template.expire("key1", 1);
            assertEquals(expire1, 1L);

            String hget10 = template.hget("key1", "f1");
            assertEquals(hget10, "v1");

            Map<String, String> hgetall5 = template.hgetAll("key1");
            assertEquals(hgetall5.size(), 4);

            Thread.sleep(2000);

            String hget11 = template.hget("key1", "f1");
            assertEquals(hget11, null);

            Map<String, String> hgetall6 = template.hgetAll("key1");
            assertEquals(hgetall6.size(), 0);
        }

        Thread.sleep(100);
        System.exit(-1);
    }

    private static void assertEquals(Object result, Object expect) throws InterruptedException {
        if (Objects.equals(result, expect)) {
            System.out.println("SUCCESS");
        } else {
            System.out.println("ERROR, expect " + expect + " but found " + result);
            Thread.sleep(100);
            System.exit(-1);
        }
    }
}
