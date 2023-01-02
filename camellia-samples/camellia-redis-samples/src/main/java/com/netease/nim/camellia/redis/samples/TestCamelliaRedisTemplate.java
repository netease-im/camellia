package com.netease.nim.camellia.redis.samples;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.util.ResourceTableUtil;
import com.netease.nim.camellia.redis.CamelliaRedisEnv;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import com.netease.nim.camellia.redis.jedis.JedisPoolFactory;
import com.netease.nim.camellia.redis.jediscluster.JedisClusterFactory;
import com.netease.nim.camellia.redis.pipeline.ICamelliaRedisPipeline;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Response;

/**
 *
 * Created by caojiajun on 2021/4/20
 */
public class TestCamelliaRedisTemplate {

    public static void test() {
        //设置连接池和超时参数
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMinIdle(0);
        jedisPoolConfig.setMaxIdle(32);
        jedisPoolConfig.setMaxTotal(32);
        jedisPoolConfig.setMaxWaitMillis(2000);
        int timeout = 2000;
        int maxAttempts = 5;
        CamelliaRedisEnv redisEnv = new CamelliaRedisEnv.Builder()
                .jedisPoolFactory(new JedisPoolFactory.DefaultJedisPoolFactory(jedisPoolConfig, timeout))
                .jedisClusterFactory(new JedisClusterFactory.DefaultJedisClusterFactory(jedisPoolConfig, timeout, timeout, maxAttempts))
                .build();

        //1、访问单点redis
        ResourceTable resourceTable = ResourceTableUtil.simpleTable(new Resource("redis://@127.0.0.1:6379"));
        //2、访问redis-sentinel
//        ResourceTable resourceTable = ResourceTableUtil.simpleTable(new Resource("redis-sentinel://passwd@127.0.0.1:16379,127.0.0.1:26379/master"));
        //3、访问redis-cluster
//        ResourceTable resourceTable = ResourceTableUtil.simpleTable(new Resource("redis-cluster://passwd@127.0.0.1:6379,127.0.0.2:6379,127.0.0.3:6379"));

        //传入CamelliaRedisEnv和ResourceTable，初始化CamelliaRedisTemplate对象
        CamelliaRedisTemplate template = new CamelliaRedisTemplate(redisEnv, resourceTable);

        //所有方法的入参和返回均和jedis保持一致
        String value = template.get("k1");
        System.out.println(value);

        //pipeline也和jedis的pipeline类似，不同点在于，每次使用完pipeline对象，务必调用close方法；可以使用try-resource语法自动close（因为ICamelliaRedisPipeline实现了Closeable接口）
        try (ICamelliaRedisPipeline pipelined = template.pipelined();) {
            Response<String> r1 = pipelined.get("k1");
            Response<Long> r2 = pipelined.hset("hk1", "hv1", "1");
            pipelined.sync();
            System.out.println(r1.get());
            System.out.println(r2.get());
        }
    }



    public static void main(String[] args) {
        test();
    }
}
