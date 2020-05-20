package com.netease.nim.camellia.redis.jedis;

import com.netease.nim.camellia.redis.conf.CamelliaRedisConstants;
import com.netease.nim.camellia.redis.proxy.CamelliaRedisProxyContext;
import com.netease.nim.camellia.redis.resource.CamelliaRedisProxyResource;
import com.netease.nim.camellia.redis.resource.RedisResource;
import com.netease.nim.camellia.redis.resource.RedisSentinelResource;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisSentinelPool;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * Created by caojiajun on 2019/7/19.
 */
public interface JedisPoolFactory {

    /**
     * 获取JedisPool对象
     * @param redisResource Redis资源定义
     * @return JedisPool对象
     */
    JedisPool getJedisPool(RedisResource redisResource);

    /**
     * 获取JedisSentinelPool对象
     * @param redisSentinelResource RedisSentinel 资源定义
     * @return JedisPool对象
     */
    JedisSentinelPool getJedisSentinelPool(RedisSentinelResource redisSentinelResource);

    /**
     * 获取CamelliaJedisPool对象
     * @param camelliaRedisProxyResource CamelliaRedisProxy资源定义
     * @return JedisPool对象
     */
    JedisPool getCamelliaJedisPool(CamelliaRedisProxyResource camelliaRedisProxyResource);

    /**
     * 一个默认实现
     */
    JedisPoolFactory DEFAULT = new DefaultJedisPoolFactory();

    /**
     * 一个默认实现
     */
    class DefaultJedisPoolFactory implements JedisPoolFactory {

        private final Object lock = new Object();
        private final ConcurrentHashMap<String, JedisPool> map1 = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, JedisSentinelPool> map2 = new ConcurrentHashMap<>();
        private final GenericObjectPoolConfig poolConfig;
        private final int timeout;

        public DefaultJedisPoolFactory() {
            this.poolConfig = new JedisPoolConfig();
            this.poolConfig.setMinIdle(CamelliaRedisConstants.Jedis.minIdle);
            this.poolConfig.setMaxTotal(CamelliaRedisConstants.Jedis.maxTotal);
            this.poolConfig.setMaxIdle(CamelliaRedisConstants.Jedis.maxIdle);
            this.poolConfig.setMaxWaitMillis(CamelliaRedisConstants.Jedis.maxWaitMillis);
            this.timeout = CamelliaRedisConstants.Jedis.timeoutMillis;
        }

        public DefaultJedisPoolFactory(GenericObjectPoolConfig poolConfig, int timeout) {
            this.poolConfig = poolConfig;
            this.timeout = timeout;
        }

        @Override
        public JedisPool getJedisPool(RedisResource redisResource) {
            JedisPool jedisPool = map1.get(redisResource.getUrl());
            if (jedisPool == null) {
                synchronized (lock) {
                    jedisPool = map1.get(redisResource.getUrl());
                    if (jedisPool == null) {
                        String password = redisResource.getPassword();
                        if (password == null || password.length() == 0) {
                            jedisPool = new JedisPool(poolConfig, redisResource.getHost(),
                                    redisResource.getPort(), timeout);
                        } else {
                            jedisPool = new JedisPool(poolConfig, redisResource.getHost(),
                                    redisResource.getPort(), timeout, redisResource.getPassword());
                        }
                        map1.put(redisResource.getUrl(), jedisPool);
                    }
                }
            }
            return jedisPool;
        }

        @Override
        public JedisSentinelPool getJedisSentinelPool(RedisSentinelResource redisSentinelResource) {
            JedisSentinelPool jedisSentinelPool = map2.get(redisSentinelResource.getUrl());
            if (jedisSentinelPool == null) {
                synchronized (lock) {
                    jedisSentinelPool = map2.get(redisSentinelResource.getUrl());
                    if (jedisSentinelPool == null) {
                        List<RedisSentinelResource.Node> nodes = redisSentinelResource.getNodes();
                        Set<String> sentinels = new HashSet<>();
                        for (RedisSentinelResource.Node node : nodes) {
                            sentinels.add(node.getHost() + ":" + node.getPort());
                        }
                        String password = redisSentinelResource.getPassword();
                        if (password == null || password.length() == 0) {
                            jedisSentinelPool = new JedisSentinelPool(redisSentinelResource.getMaster(), sentinels,
                                    poolConfig, timeout);
                        } else {
                            jedisSentinelPool = new JedisSentinelPool(redisSentinelResource.getMaster(), sentinels,
                                    poolConfig, timeout, password);
                        }
                        map2.put(redisSentinelResource.getUrl(), jedisSentinelPool);
                    }
                }
            }
            return jedisSentinelPool;
        }

        @Override
        public JedisPool getCamelliaJedisPool(CamelliaRedisProxyResource camelliaRedisProxyResource) {
            return CamelliaRedisProxyContext.getFactory().initOrGet(camelliaRedisProxyResource);
        }
    }

}
