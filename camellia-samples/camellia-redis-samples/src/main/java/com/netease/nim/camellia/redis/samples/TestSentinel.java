package com.netease.nim.camellia.redis.samples;

import com.netease.nim.camellia.redis.CamelliaRedisTemplate;

import java.util.UUID;

/**
 * Created by caojiajun on 2023/12/28
 */
public class TestSentinel {


    public static void main(String[] args) throws InterruptedException {
        CamelliaRedisTemplate template = new CamelliaRedisTemplate("redis-sentinel://pass123@10.221.145.235:16380,10.221.145.235:16381/camellia_sentinel?sentinelPassword=123");
        while (true) {
            try {
                String string = UUID.randomUUID().toString();
                String k1 = template.setex("k1", 100, string);
                System.out.println("setex=" + k1);
                String k11 = template.get("k1");
                System.out.println("get=" + k11);
            } catch (Exception e) {
                e.printStackTrace();
            }
            Thread.sleep(1000);
        }
    }
}
