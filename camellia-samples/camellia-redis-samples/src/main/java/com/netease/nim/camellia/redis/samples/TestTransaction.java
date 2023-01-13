package com.netease.nim.camellia.redis.samples;

import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Response;
import redis.clients.jedis.Transaction;

import java.util.UUID;

/**
 * Created by caojiajun on 2023/1/13
 */
public class TestTransaction {

    private static final String url = "redis://pass123@127.0.0.1:6380";

    public static void main(String[] args) {
        testTransaction(10);
    }

    public static void testTransaction(int thread) {
        CamelliaRedisTemplate template = new CamelliaRedisTemplate(url);
        for (int i=0; i<thread; i++) {
            new Thread(() -> {
                while (true) {
                    Jedis jedis = template.getJedisList().get(0);
                    try {
                        Transaction multi = jedis.multi();
                        String key = UUID.randomUUID().toString();
                        Response<String> setex = multi.setex(key, 10, UUID.randomUUID().toString());
                        System.out.println("setex=" + key);
                        Response<String> stringResponse = multi.get(key);
                        System.out.println("get=" + key);
                        multi.exec();
                        Long del = jedis.del(key);
                        System.out.println("del=" + key);
                    } finally {
                        jedis.close();
                    }
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }
}
