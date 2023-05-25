package com.netease.nim.camellia.redis.samples;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.pubsub.RedisPubSubListener;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by caojiajun on 2023/1/7
 */
public class TestPubSubTest3 {

//        private static final String uri = "redis://@127.0.0.1:6379/0";
    private static final String uri = "redis://pass123@127.0.0.1:6380/0";

    public static void main(String[] args) throws InterruptedException {
        test1(1000000);
//        test2(1000);
    }

    private static void test2(int c) {
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


    private static void test1(int c) {
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

        RedisClient redisClient2 = RedisClient.create(uri);
        new Thread(() -> {
            RedisCommands<String, String> sync = redisClient2.connect().sync();
            for (int i=0; i<c; i++) {
                try {
                    sync.publish("ch01", String.valueOf(i));
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
            if (count.get() == c) {
                System.out.println("TEST SUCCESS");
            } else {
                System.out.println("TEST FAIL!!!!!!!!!!!!!");
            }
            System.exit(-1);
        }).start();
    }
}
