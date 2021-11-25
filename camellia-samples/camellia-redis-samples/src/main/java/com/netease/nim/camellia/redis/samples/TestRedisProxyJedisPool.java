package com.netease.nim.camellia.redis.samples;

import com.netease.nim.camellia.redis.proxy.discovery.common.RegionResolver;
import com.netease.nim.camellia.redis.proxy.discovery.jedis.RedisProxyJedisPool;
import com.netease.nim.camellia.redis.proxy.discovery.zk.ZkProxyDiscovery;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPoolConfig;

public class TestRedisProxyJedisPool {

    public static void main(String[] args) {
        String zkUrl = "127.0.0.1:2181,127.0.0.2:2181";
        String basePath = "/camellia";
        String applicationName = "camellia-redis-proxy-server";
        ZkProxyDiscovery zkProxyDiscovery = new ZkProxyDiscovery(zkUrl, basePath, applicationName);

        RedisProxyJedisPool jedisPool = new RedisProxyJedisPool.Builder()
                .poolConfig(new JedisPoolConfig())
//                .bid(1)
//                .bgroup("default")
                .proxyDiscovery(zkProxyDiscovery)
                .password("pass123")
                .timeout(2000)
                .sideCarFirst(true)
                .regionResolver(new RegionResolver.IpSegmentRegionResolver("10.189.0.0/20:region1,10.189.208.0/21:region2", "default"))
//                .proxySelector(new CustomProxySelector())
                .build();

        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            jedis.setex("k1", 10, "v1");
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }
}