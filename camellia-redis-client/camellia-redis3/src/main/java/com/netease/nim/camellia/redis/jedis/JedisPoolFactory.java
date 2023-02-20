package com.netease.nim.camellia.redis.jedis;

import com.netease.nim.camellia.redis.base.proxy.Proxy;
import com.netease.nim.camellia.redis.base.resource.*;
import com.netease.nim.camellia.redis.conf.CamelliaRedisConstants;
import com.netease.nim.camellia.redis.proxy.CamelliaRedisProxyContext;
import com.netease.nim.camellia.redis.proxy.RedisProxiesContext;
import com.netease.nim.camellia.redis.proxy.discovery.common.DetectedLocalConfProxyDiscovery;
import com.netease.nim.camellia.redis.proxy.discovery.jedis.RedisProxyJedisPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisSentinelPool;

import java.util.ArrayList;
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
     * 获取JedisSentinelSlavesPool对象
     * @param redisSentinelSlavesResource RedisSentinelSlaves资源定义
     * @return JedisSentinelSlavesPool对象
     */
    JedisPool getJedisSentinelSlavesPool(RedisSentinelSlavesResource redisSentinelSlavesResource);

    /**
     * 获取RedisProxiesJedisPool对象
     * @param redisProxiesResource RedisProxiesResource资源定义
     * @return RedisProxiesJedisPool对象
     */
    JedisPool getRedisProxiesJedisPool(RedisProxiesResource redisProxiesResource);

    /**
     * 获取RedisProxiesDiscoveryJedisPool对象
     * @param redisProxiesDiscoveryResource RedisProxiesDiscoveryResource资源定义
     * @return RedisProxiesJedisPool对象
     */
    JedisPool getRedisProxiesDiscoveryJedisPool(RedisProxiesDiscoveryResource redisProxiesDiscoveryResource);

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
        private final ConcurrentHashMap<String, JedisSentinelSlavesPool> map3 = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, RedisProxyJedisPool> map4 = new ConcurrentHashMap<>();
        private final GenericObjectPoolConfig poolConfig;
        private final int timeout;
        private final long redisSentinelSlavesCheckIntervalMillis;

        public DefaultJedisPoolFactory() {
            this.poolConfig = new JedisPoolConfig();
            this.poolConfig.setMinIdle(CamelliaRedisConstants.Jedis.minIdle);
            this.poolConfig.setMaxTotal(CamelliaRedisConstants.Jedis.maxTotal);
            this.poolConfig.setMaxIdle(CamelliaRedisConstants.Jedis.maxIdle);
            this.poolConfig.setMaxWaitMillis(CamelliaRedisConstants.Jedis.maxWaitMillis);
            this.timeout = CamelliaRedisConstants.Jedis.timeoutMillis;
            this.redisSentinelSlavesCheckIntervalMillis = CamelliaRedisConstants.Jedis.redisSentinelSlavesCheckIntervalMillis;
        }

        public DefaultJedisPoolFactory(GenericObjectPoolConfig poolConfig, int timeout) {
            this.poolConfig = poolConfig;
            this.timeout = timeout;
            this.redisSentinelSlavesCheckIntervalMillis = CamelliaRedisConstants.Jedis.redisSentinelSlavesCheckIntervalMillis;
        }

        public DefaultJedisPoolFactory(GenericObjectPoolConfig poolConfig, int timeout, int redisSentinelSlavesCheckIntervalMillis) {
            this.poolConfig = poolConfig;
            this.timeout = timeout;
            this.redisSentinelSlavesCheckIntervalMillis = redisSentinelSlavesCheckIntervalMillis;
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
                                    redisResource.getPort(), timeout, null, redisResource.getDb(), null);
                        } else {
                            jedisPool = new JedisPool(poolConfig, redisResource.getHost(),
                                    redisResource.getPort(), timeout, redisResource.getPassword(), redisResource.getDb(), null);
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
                        int db = redisSentinelResource.getDb();
                        if (password == null || password.length() == 0) {
                            jedisSentinelPool = new JedisSentinelPool(redisSentinelResource.getMaster(), sentinels,
                                    poolConfig, timeout, null, db);
                        } else {
                            jedisSentinelPool = new JedisSentinelPool(redisSentinelResource.getMaster(), sentinels,
                                    poolConfig, timeout, password, db);
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

        @Override
        public JedisPool getJedisSentinelSlavesPool(RedisSentinelSlavesResource redisSentinelSlavesResource) {
            JedisSentinelSlavesPool jedisSentinelSlavesPool = map3.get(redisSentinelSlavesResource.getUrl());
            if (jedisSentinelSlavesPool == null) {
                synchronized (lock) {
                    jedisSentinelSlavesPool = map3.get(redisSentinelSlavesResource.getUrl());
                    if (jedisSentinelSlavesPool == null) {
                        jedisSentinelSlavesPool = new JedisSentinelSlavesPool(redisSentinelSlavesResource, poolConfig, timeout, redisSentinelSlavesCheckIntervalMillis);
                        map3.put(redisSentinelSlavesResource.getUrl(), jedisSentinelSlavesPool);
                    }
                }
            }
            return jedisSentinelSlavesPool;
        }

        @Override
        public JedisPool getRedisProxiesJedisPool(RedisProxiesResource redisProxiesResource) {
            RedisProxyJedisPool redisProxyJedisPool = map4.get(redisProxiesResource.getUrl());
            if (redisProxyJedisPool == null) {
                synchronized (lock) {
                    redisProxyJedisPool = map4.get(redisProxiesResource.getUrl());
                    if (redisProxyJedisPool == null) {
                        List<RedisProxiesResource.Node> nodes = redisProxiesResource.getNodes();
                        List<Proxy> list = new ArrayList<>();
                        for (RedisProxiesResource.Node node : nodes) {
                            list.add(new Proxy(node.getHost(), node.getPort()));
                        }
                        DetectedLocalConfProxyDiscovery discovery = new DetectedLocalConfProxyDiscovery(list);
                        redisProxyJedisPool = new RedisProxyJedisPool.Builder()
                                .timeout(timeout)
                                .poolConfig(poolConfig)
                                .db(redisProxiesResource.getDb())
                                .password(redisProxiesResource.getPassword())
                                .proxyDiscovery(discovery)
                                .build();
                        map4.put(redisProxiesResource.getUrl(), redisProxyJedisPool);
                    }
                }
            }
            return redisProxyJedisPool;
        }

        @Override
        public JedisPool getRedisProxiesDiscoveryJedisPool(RedisProxiesDiscoveryResource redisProxiesDiscoveryResource) {
            return RedisProxiesContext.getFactory().initOrGet(redisProxiesDiscoveryResource);
        }
    }

}
