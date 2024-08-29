package com.netease.nim.camellia.redis.proxy.kv.samples;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.core.util.ResourceTableUtil;
import com.netease.nim.camellia.redis.CamelliaRedisEnv;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import com.netease.nim.camellia.redis.jedis.JedisPoolFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by caojiajun on 2024/8/22
 */
public class TestScan {

    public static void main(String[] args) {
        String url = "redis-cluster://a32a36cb1753@10.59.135.153:6380,10.59.135.154:6380";
//        String url = "redis://@127.0.0.1:6379";
        CamelliaRedisEnv redisEnv = new CamelliaRedisEnv.Builder()
                .jedisPoolFactory(new JedisPoolFactory.DefaultJedisPoolFactory(new JedisPoolConfig(), 6000000))
                .build();
        CamelliaRedisTemplate template = new CamelliaRedisTemplate(redisEnv, ResourceTableUtil.simpleTable(new Resource(url)));

        Set<String> set = new HashSet<>();
        String cursor = "0";
        ScanParams scanParams = new ScanParams();
        scanParams.count(100);
        while (true) {
            try (Jedis readJedis = template.getReadJedis("")) {
                ScanResult<String> result = readJedis.scan(cursor, scanParams);
                cursor = result.getCursor();
                System.out.println("cursor=" + cursor);
                if (set.contains(cursor)) {
                    System.out.println("duplicate cursor = " + cursor);
                    System.exit(-1);
                }
                set.add(cursor);
                List<String> result1 = result.getResult();
                for (String key : result1) {
                    try {
                        Set<String> zrange = template.zrange(key, 0, -1);
                        template.zremrangeByRank(key, 0, -101);
                        template.zrangeByScore(key, 0, System.currentTimeMillis());
                        System.out.println("key = " + key + ",size=" + zrange.size());
                        if (zrange.size() > 1000) {
                            System.exit(-1);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.out.println("key=" + key);
                    }
                }
                template.del(result1.toArray(new String[0]));

                if (result.isCompleteIteration()) {
                    break;
                }
            }
        }
        System.out.println("SUCCESS");
    }
}
