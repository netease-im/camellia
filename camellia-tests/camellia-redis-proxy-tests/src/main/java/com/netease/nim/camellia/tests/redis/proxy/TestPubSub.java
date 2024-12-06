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
    private static final String ch3 = "ch3";

    public static void main(String[] args) throws Exception {
        String url = "redis://pass123@127.0.0.1:6380";
//        url = "redis://@127.0.0.1:6379";
        CamelliaRedisTemplate template = new CamelliaRedisTemplate(url);
        MyJedisPubSub myJedisPubSub = new MyJedisPubSub();
        Jedis jedis = template.getWriteJedis("");
        new Thread(() -> {
            jedis.subscribe(myJedisPubSub,"ch1", "ch2");
        }).start();

        Thread.sleep(100);

        CountDownLatch latch1 = new CountDownLatch(2);
        new Thread(() -> {
            try {
                for (int i=1; i<500; i++) {
                    try (Jedis jedis1 = template.getWriteJedis("")) {
                        jedis1.publish(ch1, "ch1-msg-" + i);
                    }
                }
                latch1.countDown();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
        new Thread(() -> {
            try {
                for (int i=1; i<500; i++) {
                    try (Jedis jedis2 = template.getWriteJedis("")) {
                        jedis2.publish(ch2, "ch2-msg-" + i);
                    }
                }
                latch1.countDown();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
        latch1.await();

        Thread.sleep(1000);

        assertEquals(myJedisPubSub.getC1(), 499L);
        assertEquals(myJedisPubSub.getC2(), 499L);

        myJedisPubSub.reset();

        myJedisPubSub.unsubscribe("ch1");

        CountDownLatch latch2 = new CountDownLatch(1);
        new Thread(() -> {
            try {
                for (int i=1; i<500; i++) {
                    try (Jedis jedis3 = template.getWriteJedis("")) {
                        jedis3.publish(ch2, "ch2-msg-" + i);
                    }
                }
                latch2.countDown();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
        latch2.await();

        Thread.sleep(1000);

        assertEquals(myJedisPubSub.getC2(), 499L);

        myJedisPubSub.reset();

        myJedisPubSub.subscribe("ch3");

        CountDownLatch latch3 = new CountDownLatch(2);
        new Thread(() -> {
            try {
                for (int i=1; i<500; i++) {
                    try (Jedis jedis4 = template.getWriteJedis("")) {
                        jedis4.publish(ch2, "ch2-msg-" + i);
                    }
                }
                latch3.countDown();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
        new Thread(() -> {
            try {
                for (int i=1; i<500; i++) {
                    try (Jedis jedis5 = template.getWriteJedis("")) {
                        jedis5.publish(ch3, "ch3-msg-" + i);
                    }
                }
                latch3.countDown();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        latch3.await();

        Thread.sleep(100);

        assertEquals(myJedisPubSub.getC2(), 499L);
        assertEquals(myJedisPubSub.getC3(), 499L);

        myJedisPubSub.reset();

        myJedisPubSub.unsubscribe("ch1", "ch2", "ch3");

        Thread.sleep(1000);

        assertEquals(jedis.setex("k1", 100, "v1"), "OK");
        assertEquals(jedis.get("k1"), "v1");

        System.out.println("SUCCESS");
        Thread.sleep(100);
        System.exit(-1);
    }

    public static class MyJedisPubSub extends JedisPubSub {

        private static final AtomicLong c1 = new AtomicLong();
        private static final AtomicLong c2 = new AtomicLong();
        private static final AtomicLong c3 = new AtomicLong();

        public long getC1() {
            return c1.get();
        }

        public long getC2() {
            return c2.get();
        }

        public long getC3() {
            return c3.get();
        }

        public void reset() {
            c1.set(0);
            c2.set(0);
            c3.set(0);
        }

        @Override
        public void onMessage(String channel, String message) {
            if (channel.equalsIgnoreCase(ch1)) {
                long id = c1.incrementAndGet();
                assertEquals(message, "ch1-msg-" + id);
            } else if (channel.equalsIgnoreCase(ch2)) {
                long id = c2.incrementAndGet();
                assertEquals(message, "ch2-msg-" + id);
            } else if (channel.equalsIgnoreCase(ch3)) {
                long id = c3.incrementAndGet();
                assertEquals(message, "ch3-msg-" + id);
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
