package com.netease.nim.camellia.redis.proxy.kv.samples;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.core.util.ResourceTableUtil;
import com.netease.nim.camellia.redis.CamelliaRedisEnv;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import com.netease.nim.camellia.redis.jedis.JedisPoolFactory;
import com.netease.nim.camellia.redis.jediscluster.JedisClusterFactory;
import com.netease.nim.camellia.tools.statistic.CamelliaStatisticsManager;
import com.netease.nim.camellia.tools.statistic.CamelliaStatsData;
import redis.clients.jedis.JedisPoolConfig;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Created by caojiajun on 2024/8/13
 */
public class PerformanceTest {

    private static final String prefix = "def";

    private static final String url = "redis://pass123@127.0.0.1:6381";
//    private static final String url = "redis://@127.0.0.1:6379";
    private static CamelliaRedisTemplate template;

    public static int readThread = 1;
    public static int writeThread = 1;

    public static int keySize = 1000;

    public static int stringSize = 200;

    public static int hashSize = 100;
    public static int hashValueSize = 100;

    private static String setPrefix = null;
    public static int setSize = 100;
    public static int setValueSize = 100;

    public static int zsetSize = 100;
    public static int zsetValueSize = 200;

    public static void main(String[] args) {
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxIdle(32);
        jedisPoolConfig.setMinIdle(0);
        jedisPoolConfig.setMaxTotal(32);
        jedisPoolConfig.setMaxWaitMillis(3000);
        CamelliaRedisEnv redisEnv = new CamelliaRedisEnv.Builder()
                .jedisPoolFactory(new JedisPoolFactory.DefaultJedisPoolFactory(new JedisPoolConfig(), 6000000))
                .jedisClusterFactory(new JedisClusterFactory.DefaultJedisClusterFactory(new JedisPoolConfig(), 6000000, 6000000, 5))
                .build();
        template = new CamelliaRedisTemplate(redisEnv, ResourceTableUtil.simpleTable(new Resource(url)));

        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            Map<String, CamelliaStatsData> map = manager.getStatsDataAndReset();
            System.out.println("=====");
            for (Map.Entry<String, CamelliaStatsData> entry : map.entrySet()) {
                CamelliaStatsData value = entry.getValue();
                System.out.println("command=" + entry.getKey() + ",qps=" + value.getCount() / 10
                        + ",avgMs=" + value.getAvg() + ",maxMs=" + value.getMax() + ",p50="
                        + value.getP50() + ",p90=" + value.getP90() + ",p99=" + value.getP99());
            }
        }, 10, 10, TimeUnit.SECONDS);

        test();
    }

    private static void test() {
        setPrefix = value(setValueSize);

        for (int i=0; i<readThread; i++) {
            new Thread(() -> {
                while (true) {
                    testStringRead();
                }
            }).start();
            new Thread(() -> {
                while (true) {
                    testHashRead();
                }
            }).start();
            new Thread(() -> {
                while (true) {
                    testSetRead();
                }
            }).start();
            new Thread(() -> {
                while (true) {
                    testZSetRead();
                }
            }).start();
        }
        for (int i=0; i<writeThread; i++) {
            new Thread(() -> {
                while (true) {
                    testStringWrite();
                }
            }).start();
            new Thread(() -> {
                while (true) {
                    testHashWrite();
                }
            }).start();
            new Thread(() -> {
                while (true) {
                    testSetWrite();
                }
            }).start();
            new Thread(() -> {
                while (true) {
                    testZSetWrite();
                }
            }).start();
        }
    }

    private static final CamelliaStatisticsManager manager = new CamelliaStatisticsManager();

    private static void testHashWrite() {
        String key = key("hash");
        String field = prefix + "|" + ThreadLocalRandom.current().nextInt(hashSize);
        String value = value(hashValueSize);
        long start1 = System.currentTimeMillis();
        try {
            template.hset(key, field, value);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            manager.update("hset", System.currentTimeMillis() - start1);
        }
        long start2 = System.currentTimeMillis();
        try {
            template.expire(key, 60);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            manager.update("expire", System.currentTimeMillis() - start2);
        }
    }

    private static void testHashRead() {
        String key = key("hash");
        long start = System.currentTimeMillis();
        try {
            template.hgetAll(key);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            manager.update("hgetAll", System.currentTimeMillis() - start);
        }
    }

    private static void testStringWrite() {
        String key = key("string");
        String value = value(stringSize);
        long start = System.currentTimeMillis();
        try {
            template.setex(key, 60, value);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            manager.update("setex", System.currentTimeMillis() - start);
        }
    }

    private static void testStringRead() {
        String key = key("string");
        long start = System.currentTimeMillis();
        try {
            template.get(key);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            manager.update("get", System.currentTimeMillis() - start);
        }
    }

    private static void testSetWrite() {
        String key = key("set");
        String member = setPrefix + "|" + ThreadLocalRandom.current().nextInt(setSize);
        long start = System.currentTimeMillis();
        try {
            template.sadd(key, member);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            manager.update("sadd", System.currentTimeMillis() - start);
        }
        long start2 = System.currentTimeMillis();
        try {
            template.expire(key, 60);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            manager.update("expire", System.currentTimeMillis() - start2);
        }
    }

    private static void testSetRead() {
        String key = prefix + ThreadLocalRandom.current().nextInt(keySize);
        long start = System.currentTimeMillis();
        try {
            template.smembers(key);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            manager.update("smembers", System.currentTimeMillis() - start);
        }
    }

    private static void testZSetWrite() {
        String key = key("zset");
        String member = value(zsetValueSize);
        long score = System.currentTimeMillis();
        long start1 = System.currentTimeMillis();
        try {
            template.zadd(key, score, member);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            manager.update("zadd", System.currentTimeMillis() - start1);
        }
        long start2 = System.currentTimeMillis();
        try {
            template.zremrangeByRank(key, 0, -zsetSize - 1);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            manager.update("zremrangeByRank", System.currentTimeMillis() - start2);
        }
        long start3 = System.currentTimeMillis();
        try {
            template.expire(key, 60);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            manager.update("expire", System.currentTimeMillis() - start3);
        }
    }

    private static void testZSetRead() {
        String key = key("zset");
        long start = System.currentTimeMillis();
        try {
            template.zrangeByScore(key, 0, System.currentTimeMillis());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            manager.update("zrangeByScore", System.currentTimeMillis() - start);
        }
    }

    private static String key(String type) {
        return prefix + "|" + type + "|" + ThreadLocalRandom.current().nextInt(keySize);
    }

    private static String value(int size) {
        StringBuilder builder = new StringBuilder();
        String string = UUID.randomUUID().toString();
        for (int i=0; i<size; i++) {
            builder.append(string.charAt(ThreadLocalRandom.current().nextInt(string.length())));
        }
        return builder.toString();
    }
}
