package com.netease.nim.camellia.redis.samples;

import com.netease.nim.camellia.tools.ssl.SSLContextUtil;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

/**
 * Created by caojiajun on 2023/8/14
 */
public class TestTlsJedis {

    public static void main(String[] args) {
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        SSLContext sslContext = SSLContextUtil.genSSLContext("/Users/caojiajun/tools/redis-7.0.11/tests/tls/ca.crt",
                "/Users/caojiajun/tools/redis-7.0.11/tests/tls/redis.crt", "/Users/caojiajun/tools/redis-7.0.11/tests/tls/redis.key", null);
        SSLParameters sslParameters = new SSLParameters();
        JedisPool jedisPool = new JedisPool(jedisPoolConfig, "127.0.0.1", 6381, 2000, "pass123",
                0, true, sslContext.getSocketFactory(), sslParameters, (s, sslSession) -> true);
        try (Jedis jedis = jedisPool.getResource()) {
            String value = jedis.get("k1");
            System.out.println(value);
        }
    }
}
