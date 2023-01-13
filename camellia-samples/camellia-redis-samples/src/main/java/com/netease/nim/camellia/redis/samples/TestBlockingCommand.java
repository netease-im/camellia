package com.netease.nim.camellia.redis.samples;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import redis.clients.jedis.Jedis;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by caojiajun on 2023/1/9
 */
public class TestBlockingCommand {

    private static final String url = "redis://pass123@127.0.0.1:6380";

    public static void main(String[] args) {
        testProduce();
        testConsume();
    }

    public static void testProduce() {
        new Thread(() -> {
            CamelliaRedisTemplate template = new CamelliaRedisTemplate(url);
            AtomicLong data = new AtomicLong();
            while (true) {
                template.lpush("lk1", String.valueOf(data.incrementAndGet()));
                System.out.println("produce: lpush lk1=" + data.get());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public static void testConsume() {
        new Thread(() -> {
            CamelliaRedisTemplate template = new CamelliaRedisTemplate(url);
            while (true) {
                try (Jedis jedis = template.getJedisList().get(0)) {
                    List<String> lk1 = jedis.blpop(10, "lk1");
                    System.out.println("consume: " + JSONObject.toJSON(lk1));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

}
