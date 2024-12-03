package com.netease.nim.camellia.tests.redis.proxy;


import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by caojiajun on 2024/12/3
 */
public class TestPubSub {

    private static final String ch1 = "ch1";
    private static final String ch2 = "ch2";

    public static void main(String[] args) throws Exception {
        CamelliaRedisTemplate template = new CamelliaRedisTemplate("redis://pass123@127.0.0.1:6380");
        new Thread(() -> {
            Jedis jedis = template.getWriteJedis("");
            jedis.subscribe(new MyJedisPubSub(),"ch1", "ch2");
        }).start();

        Thread.sleep(100);

        CountDownLatch latch = new CountDownLatch(2);
        new Thread(() -> {
            try {
                for (int i=1; i<500; i++) {
                    try (Jedis jedis = template.getWriteJedis("")) {
                        jedis.publish(ch1, "ch1-msg-" + i);
                    }
                }
                latch.countDown();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
        new Thread(() -> {
            try {
                for (int i=1; i<500; i++) {
                    try (Jedis jedis = template.getWriteJedis("")) {
                        jedis.publish(ch2, "ch2-msg-" + i);
                    }
                }
                latch.countDown();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
        latch.await();
        System.out.println("SUCCESS");
        Thread.sleep(1000);
        System.exit(-1);
    }

    public static class MyJedisPubSub extends JedisPubSub {

        private static final AtomicLong c1 = new AtomicLong();
        private static final AtomicLong c2 = new AtomicLong();

        @Override
        public void onMessage(String channel, String message) {
            if (channel.equalsIgnoreCase(ch1)) {
                long id = c1.incrementAndGet();
                assertEquals(message, "ch1-msg-" + id);
            } else if (channel.equalsIgnoreCase(ch2)) {
                long id = c2.incrementAndGet();
                assertEquals(message, "ch2-msg-" + id);
            } else {
                System.out.println("ERROR");
                System.exit(-1);
            }
        }
    }

    private static void assertEquals(Object result, Object expect) {
        if (Objects.equals(result, expect)) {
            System.out.println("SUCCESS, thread=" + Thread.currentThread().getName());
        } else {
            System.out.println("ERROR, expect " + expect + " but found " + result);
            throw new RuntimeException();
        }
    }
}
