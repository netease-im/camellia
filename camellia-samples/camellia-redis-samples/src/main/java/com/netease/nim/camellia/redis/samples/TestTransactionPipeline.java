package com.netease.nim.camellia.redis.samples;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.util.ResourceTableUtil;
import com.netease.nim.camellia.redis.CamelliaRedisEnv;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import com.netease.nim.camellia.redis.base.resource.RedisResource;
import com.netease.nim.camellia.redis.base.resource.RedisResourceUtil;
import com.netease.nim.camellia.redis.jedis.JedisPoolFactory;
import com.netease.nim.camellia.redis.jediscluster.JedisClusterFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Pipeline;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by caojiajun on 2023/4/12
 */
public class TestTransactionPipeline {

    public static void main(String[] args) {

//        String url = "redis://@127.0.0.1:6379";
        String url = "redis://pass123@127.0.0.1:6380";
//        String url = "redis-cluster://c17b87cda@10.200.132.167:6381,10.200.132.169:6381";
        ResourceTable resourceTable = ResourceTableUtil.simpleTable(RedisResourceUtil.parseResourceByUrl(new Resource(url)));

        CamelliaRedisEnv redisEnv = new CamelliaRedisEnv.Builder()
                .jedisPoolFactory(new JedisPoolFactory.DefaultJedisPoolFactory(new JedisPoolConfig(), 60*1000*10))
                .jedisClusterFactory(new JedisClusterFactory.DefaultJedisClusterFactory(new JedisPoolConfig(), 60*1000*10, 60*1000*10, 5))
                .build();

        CamelliaRedisTemplate template = new CamelliaRedisTemplate(redisEnv, resourceTable);
        String key = UUID.randomUUID().toString();
        Jedis jedis = template.getReadJedis(key);
        assertEquals(Long.valueOf(5L), jedis.incrBy(key, 5L));

        List<Object> expect = new ArrayList<>();
        List<Object> expMulti = new ArrayList<>();

        Pipeline pipe = jedis.pipelined();
        pipe.incrBy(key, 3L);   expect.add(8L);
        pipe.watch(key);        expect.add("OK");
        pipe.multi();           expect.add("OK");
        pipe.incrBy(key, 6L);   expect.add("QUEUED"); expMulti.add(14L);
        pipe.decrBy(key, 2L);   expect.add("QUEUED"); expMulti.add(12);

        pipe.exec();            expect.add(expMulti); // success MULTI
        pipe.incrBy(key, 7L);   expect.add(19L);
        assertEquals(expect, pipe.syncAndReturnAll());
//        jedis.incrBy(key, 7L);   expect.add(19L);
        assertEquals(Long.valueOf(23L), jedis.incrBy(key, 4L));

    }

    private static void assertEquals(Object o1, Object o2) {
        System.out.println(o1 + " <--> " + o2);
    }
}
