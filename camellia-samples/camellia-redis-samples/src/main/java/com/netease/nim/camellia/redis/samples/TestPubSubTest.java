package com.netease.nim.camellia.redis.samples;

import com.alibaba.fastjson.JSONObject;
import io.lettuce.core.KeyValue;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.pubsub.RedisPubSubListener;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by caojiajun on 2023/1/7
 */
public class TestPubSubTest {

//        private static final String uri = "redis://@127.0.0.1:6379/0";
    private static final String uri = "redis://pass123@127.0.0.1:6380/0";

    public static void main(String[] args) throws InterruptedException {
//        test1(1000);
        test2(1000);
//        test3();
//        test4();
    }

    public static void test4() {
        try {
            pub();
            sub();
            System.out.println("TEST SUCCESS");
            System.exit(-1);
        } catch (Exception e) {
            System.out.println("TEST FAIL!!!!!!!!!!!!!");
        }
    }

    public static void test3() {
        Jedis jedis = new Jedis("127.0.0.1", 6380);
        jedis.auth("pass123");

        try {
            jedis.subscribe(new JedisPubSub() {
                @Override
                public void onMessage(String channel, String message) {
                    System.out.println("onMessage " + channel + " " + message);
                }
            }, "ch1");
        } catch (Exception e) {
            System.out.println("error1:" + e);
            for (int i=0; i<10; i++) {
                try {
                    String k1 = jedis.get("k1");
                    System.out.println(k1);
                } catch (Exception ex) {
                    System.out.println("error2:" + ex);
                }
            }
        }
    }

    public static void test2(int c) {
        AtomicLong count = new AtomicLong();
        RedisClient redisClient1 = RedisClient.create(uri);
        RedisClient redisClient2 = RedisClient.create(uri);
        StatefulRedisPubSubConnection<String, String> connection = redisClient1.connectPubSub();
        connection.addListener(new RedisPubSubListener<String, String>() {
            @Override
            public void message(String channel, String message) {
                System.out.println("message,channel=" + channel + ",message=" + message);
                count.incrementAndGet();
            }

            @Override
            public void message(String pattern, String channel, String message) {
                System.out.println("message,pattern=" + pattern + ",channel=" + channel + ",message=" + message);
            }

            @Override
            public void subscribed(String channel, long count) {
                System.out.println("subscribed,channel=" + channel + ",count=" + count);
            }

            @Override
            public void psubscribed(String pattern, long count) {
                System.out.println("psubscribed,pattern=" + pattern + ",count=" + count);
            }

            @Override
            public void unsubscribed(String channel, long count) {
                System.out.println("unsubscribed,channel=" + channel + ",count=" + count);
            }

            @Override
            public void punsubscribed(String pattern, long count) {
                System.out.println("punsubscribed,pattern=" + pattern + ",count=" + count);
            }
        });
        System.out.println("try subscribe ch01");
        connection.async().subscribe("ch01");
        System.out.println("subscribe ch01 done");

        new Thread(() -> {
            while (true) {
                try {
                    connection.async().subscribe("ch02");
                    Thread.sleep(10);
                    connection.async().unsubscribe("ch02");
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        new Thread(() -> {
            RedisCommands<String, String> sync = redisClient2.connect().sync();
            for (int i=0; i<c; i++) {
                sync.publish("ch01", String.valueOf(i));
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (count.get() == c) {
                System.out.println("TEST SUCCESS");
            } else {
                System.out.println("TEST FAIL!!!!!!!!!!!!!");
            }
            System.exit(-1);
        }).start();
    }


    public static void test1(int c) throws InterruptedException {
        AtomicLong count = new AtomicLong();

        RedisClient redisClient1 = RedisClient.create(uri);
        StatefulRedisPubSubConnection<String, String> connection = redisClient1.connectPubSub();
        connection.addListener(new RedisPubSubListener<String, String>() {
            @Override
            public void message(String channel, String message) {
                System.out.println("message,channel=" + channel + ",message=" + message);
                count.incrementAndGet();
            }

            @Override
            public void message(String pattern, String channel, String message) {
                System.out.println("message,pattern=" + pattern + ",channel=" + channel + ",message=" + message);
            }

            @Override
            public void subscribed(String channel, long count) {
                System.out.println("subscribed,channel=" + channel + ",count=" + count);
            }

            @Override
            public void psubscribed(String pattern, long count) {
                System.out.println("psubscribed,pattern=" + pattern + ",count=" + count);
            }

            @Override
            public void unsubscribed(String channel, long count) {
                System.out.println("unsubscribed,channel=" + channel + ",count=" + count);
            }

            @Override
            public void punsubscribed(String pattern, long count) {
                System.out.println("punsubscribed,pattern=" + pattern + ",count=" + count);
            }
        });
        System.out.println("try subscribe ch01");
        connection.async().subscribe("ch01");
        System.out.println("subscribe ch01 done");
        RedisClient redisClient3 = RedisClient.create(uri);
        for (int i=0; i<100; i++) {
            redisClient3.connect().sync().publish("ch01", "abc");
        }
        Thread.sleep(1000);
        count.set(0);
        System.out.println("try subscribe ch01");
        connection.async().subscribe("ch02");
        System.out.println("subscribe ch02 done");

        RedisClient redisClient2 = RedisClient.create(uri);
        new Thread(() -> {
            RedisCommands<String, String> sync = redisClient2.connect().sync();
            for (int i=0; i<c; i++) {
                try {
                    sync.publish("ch01", String.valueOf(i));
                    sync.publish("ch02", String.valueOf(i));
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (count.get() == c*2L) {
                System.out.println("TEST SUCCESS");
            } else {
                System.out.println("TEST FAIL!!!!!!!!!!!!!");
            }
            System.exit(-1);
        }).start();
    }


    public static void pub() {
        new Thread(() -> {
            RedisClient redisClient1 = RedisClient.create(uri);
            RedisCommands<String, String> sync = redisClient1.connect().sync();
            for (int i=0; i<100000; i++) {
                sync.publish("ch01", String.valueOf(i));
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }

    public static void sub() {
        try {
            RedisClient redisClient2 = RedisClient.create(uri);
            StatefulRedisPubSubConnection<String, String> connection = redisClient2.connectPubSub();
            connection.addListener(new RedisPubSubListener<String, String>() {


                @Override
                public void message(String channel, String message) {
                    System.out.println("message,channel=" + channel + ",message=" + message);
                }

                @Override
                public void message(String pattern, String channel, String message) {
                    System.out.println("message,pattern=" + pattern + ",channel=" + channel + ",message=" + message);
                }

                @Override
                public void subscribed(String channel, long count) {
                    System.out.println("subscribed,channel=" + channel + ",count=" + count);
                }

                @Override
                public void psubscribed(String pattern, long count) {
                    System.out.println("psubscribed,pattern=" + pattern + ",count=" + count);
                }

                @Override
                public void unsubscribed(String channel, long count) {
                    System.out.println("unsubscribed,channel=" + channel + ",count=" + count);
                }

                @Override
                public void punsubscribed(String pattern, long count) {
                    System.out.println("punsubscribed,pattern=" + pattern + ",count=" + count);
                }
            });

            System.out.println("try subscribe ch01");
            connection.async().subscribe("ch01");
            System.out.println("subscribe ch01 done");

            Thread.sleep(1000);
            System.out.println("try unsubscribe ch01");
            connection.async().unsubscribe("ch01");
            System.out.println("unsubscribe ch01 done");

            Thread.sleep(1000);

            System.out.println("try subscribe2 ch01");
            connection.async().subscribe("ch01");
            System.out.println("subscribe2 ch01 done");

            Thread.sleep(1000);
            System.out.println("try unsubscribe2 ch01");
            connection.async().unsubscribe("ch01");
            System.out.println("unsubscribe2 ch01 done");

            Thread.sleep(1000);
            String ping = connection.sync().ping();
            System.out.println("ping=" + ping);

            Thread.sleep(1000);

            System.out.println("try subscribe2 ch01");
            connection.async().subscribe("ch01");
            System.out.println("subscribe2 ch01 done");
            Thread.sleep(5000);
            System.out.println("try unsubscribe3 ch01");
            connection.async().unsubscribe("ch01");
            System.out.println("unsubscribe3 ch01 done");

            Thread.sleep(1000);
            String ping2 = connection.sync().ping();
            System.out.println("ping2=" + ping2);

            System.out.println("try subscribe2 ch01");
            connection.async().subscribe("ch01");
            System.out.println("subscribe2 ch01 done");
            Thread.sleep(20000);
            System.out.println("try unsubscribe3 ch01");
            connection.async().unsubscribe("ch01");
            System.out.println("unsubscribe3 ch01 done");

            Thread.sleep(1000);
            String ping3 = connection.sync().ping();
            System.out.println("ping3=" + ping3);

            connection.sync().lpush("lk01", "v1");
            long start1 = System.currentTimeMillis();
            KeyValue<String, String> kv1 = connection.sync().blpop(10, "lk01");
            System.out.println(JSONObject.toJSONString(kv1) + ",spend=" + (System.currentTimeMillis() - start1));
            long start2 = System.currentTimeMillis();
            KeyValue<String, String> kv2 = connection.sync().blpop(10, "lk01");
            System.out.println(JSONObject.toJSONString(kv2) + ",spend=" + (System.currentTimeMillis() - start2));

            System.out.println("=========end==========");
        } catch (Exception e) {
            System.out.println("TEST FAIL!!!!!!!!!!!!!");
        }
    }
}
