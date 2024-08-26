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
        String url = "redis://pass123@127.0.0.1:6381";
//        String url = "redis-cluster://a32a36cb1753@10.59.135.153:6380,10.59.135.154:6380";//v0
//        String url = "redis-cluster://dde946f2933e@10.59.135.153:6380,10.59.135.154:6380";//v1
//        String url = "redis://@127.0.0.1:6379";
        CamelliaRedisEnv redisEnv = new CamelliaRedisEnv.Builder()
                .jedisPoolFactory(new JedisPoolFactory.DefaultJedisPoolFactory(new JedisPoolConfig(), 6000))
                .jedisClusterFactory(new JedisClusterFactory.DefaultJedisClusterFactory(new JedisPoolConfig(), 6000, 6000, 5))
                .build();
        CamelliaRedisTemplate template = new CamelliaRedisTemplate(redisEnv, ResourceTableUtil.simpleTable(new Resource(url)));

        int threads = 3;

        int stringVersion = 0;
        int hashVersion = 0;
        int zsetVersion = 0;
        int setVersion = 0;

        for (int i = 0; i<threads; i++) {
            new Thread(() -> {
                while (true) {
                    if (hashVersion == 0) {
                        TestHashV0.testHash(template);
                    } else if (hashVersion == 1){
                        TestHashV1.testHash(template);
                    }
                    sleep(100);
                }
            }).start();

            new Thread(() -> {
                while (true) {
                    if (zsetVersion == 0) {
                        TestZSetV0.testZSet(template);
                    } else if (zsetVersion == 1) {
                        TestZSetV1.testZSet(template);
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

            new Thread(() -> {
                while (true) {
                    if (setVersion == 0) {
                        TestSetV0.testSet(template);
                    } else if (setVersion == 1) {
                        TestSetV1.testSet(template);
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
