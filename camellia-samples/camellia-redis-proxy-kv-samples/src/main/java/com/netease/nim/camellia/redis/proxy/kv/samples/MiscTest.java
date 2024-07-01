package com.netease.nim.camellia.redis.proxy.kv.samples;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.core.util.ResourceTableUtil;
import com.netease.nim.camellia.redis.CamelliaRedisEnv;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import com.netease.nim.camellia.redis.jedis.JedisPoolFactory;
import com.netease.nim.camellia.redis.jediscluster.JedisClusterFactory;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Created by caojiajun on 2024/6/5
 */
public class MiscTest {

    public static void main(String[] args) {
//        String url = "redis://pass123@127.0.0.1:6381";
        String url = "redis-cluster://7aab8fcd9@10.189.31.11:6382,10.189.31.13:6382";
//        String url = "redis://@127.0.0.1:6379";
        CamelliaRedisEnv redisEnv = new CamelliaRedisEnv.Builder()
                .jedisPoolFactory(new JedisPoolFactory.DefaultJedisPoolFactory(new JedisPoolConfig(), 6000000))
                .jedisClusterFactory(new JedisClusterFactory.DefaultJedisClusterFactory(new JedisPoolConfig(), 6000000, 6000000, 5))
                .build();
        CamelliaRedisTemplate template = new CamelliaRedisTemplate(redisEnv, ResourceTableUtil.simpleTable(new Resource(url)));

        int threads = 3;

        int hashVersion = 0;
        int zsetVersion = 0;
        int stringVersion = 0;

        for (int i = 0; i<threads; i++) {
            new Thread(() -> {
                while (true) {
                    if (hashVersion == 0 || hashVersion == 2) {
                        TestHashV0V2.testHash(template);
                    } else if (hashVersion == 1 || hashVersion == 3){
                        TestHashV1V3.testHash(template);
                    }
                    sleep(100);
                }
            }).start();

            new Thread(() -> {
                while (true) {
                    if (zsetVersion == 0 || zsetVersion == 1 || zsetVersion == 2) {
                        TestZSetV0V1V2.testZSet(template);
                    } else if (zsetVersion == 3) {
                        TestZSetV3.testZSet(template);
                    }
                    sleep(100);
                }
            }).start();

            new Thread(() -> {
                while (true) {
                    if (stringVersion == 0) {
                        TestStringV0.testString(template);
                    }
                    sleep(100);
                }
            }).start();
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


}
