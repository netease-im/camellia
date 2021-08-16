package com.netease.nim.camellia.redis.samples;

import com.netease.nim.camellia.redis.CamelliaRedisEnv;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;

/**
 *
 * Created by caojiajun on 2021/7/30
 */
public class TestRedisTemplateResourceTableUpdater {

    public static void main(String[] args) throws InterruptedException {
        CamelliaRedisTemplate template = new CamelliaRedisTemplate(CamelliaRedisEnv.defaultRedisEnv(), new CustomRedisTemplateResourceTableUpdater());
        while (true) {
            System.out.println(template.get("k1"));
            Thread.sleep(1000);
        }
    }
}
