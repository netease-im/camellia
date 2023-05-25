package com.netease.nim.camellia.redis.samples;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.pubsub.RedisPubSubListener;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

/**
 * Created by caojiajun on 2023/1/7
 */
public class TestPubSubLettuce2 {

//    private static final String uri = "redis://@127.0.0.1:6379/0";
    private static final String uri = "redis://pass123@127.0.0.1:6380/0";
    private static final RedisClient redisClient1 = RedisClient.create(uri);
    private static final RedisClient redisClient2 = RedisClient.create(uri);

    public static void main(String[] args) throws InterruptedException {
//        pub();
//        sub();

        Jedis jedis = new Jedis("127.0.0.1", 6380);
        jedis.auth("pass123");
        new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("sub");
                jedis.subscribe(new JedisPubSub() {
                    @Override
                    public void onMessage(String channel, String message) {
                        System.out.println("receive " + channel + " " + message);
                    }
                }, "ch01");
            }
        }).start();

        Thread.sleep(5000);
        for (int i=0; i<100; i++) {
            try {
                System.out.println(jedis.ping());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void pub() {
        new Thread(() -> {
            RedisCommands<String, String> sync = redisClient1.connect().sync();
            sync.lpush("bk1", "abc");
            for (int i=0; i<100000; i++) {
                sync.publish("ch01", String.valueOf(i));
                sync.publish("ch02", String.valueOf(i));
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }

    public static void sub() {
        try {
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

            Thread.sleep(5000);

            for (int i=0; i<40; i++) {
                connection.async().ping();
            }

            connection.close();
            System.out.println("=========end==========");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
