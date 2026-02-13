package com.netease.nim.camellia.redis.jedis;

import com.netease.nim.camellia.core.discovery.DetectedLocalConfCamelliaDiscovery;
import com.netease.nim.camellia.core.discovery.ServerNode;
import com.netease.nim.camellia.redis.base.resource.*;
import com.netease.nim.camellia.redis.conf.CamelliaRedisConstants;
import com.netease.nim.camellia.redis.proxy.CamelliaRedisProxyContext;
import com.netease.nim.camellia.redis.proxy.RedisProxiesContext;
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
     * @param resource Redis资源定义
     * @return JedisPool对象
     */
    JedisPool getJedisPool(RedisResource resource);

    /**
     * 获取JedisSentinelPool对象
     * @param resource RedisSentinel 资源定义
     * @return JedisPool对象
     */
    JedisSentinelPool getJedisSentinelPool(RedisSentinelResource resource);

    /**
     * 获取CamelliaJedisPool对象
     * @param resource CamelliaRedisProxy资源定义
     * @return JedisPool对象
     */
    JedisPool getCamelliaJedisPool(CamelliaRedisProxyResource resource);

    /**
     * 获取JedisSentinelSlavesPool对象
     * @param resource RedisSentinelSlaves资源定义
     * @return JedisSentinelSlavesPool对象
     */
    JedisPool getJedisSentinelSlavesPool(RedisSentinelSlavesResource resource);

    /**
     * 获取RedisProxiesJedisPool对象
     * @param resource RedisProxiesResource资源定义
     * @return RedisProxiesJedisPool对象
     */
    JedisPool getRedisProxiesJedisPool(RedisProxiesResource resource);

    /**
     * 获取RedisProxiesDiscoveryJedisPool对象
     * @param resource RedisProxiesDiscoveryResource资源定义
     * @return RedisProxiesJedisPool对象
     */
    JedisPool getRedisProxiesDiscoveryJedisPool(RedisProxiesDiscoveryResource resource);

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
        public JedisPool getJedisPool(RedisResource resource) {
            JedisPool jedisPool = map1.get(resource.getUrl());
            if (jedisPool == null) {
                synchronized (lock) {
                    jedisPool = map1.get(resource.getUrl());
                    if (jedisPool == null) {
                        String password = resource.getPassword();
                        if (password == null || password.length() == 0) {
                            password = null;
                        }
                        jedisPool = new JedisPool(poolConfig, resource.getHost(),
                                resource.getPort(), timeout, resource.getUserName(), password, resource.getDb(), null);
                        map1.put(resource.getUrl(), jedisPool);
                    }
                }
            }
            return jedisPool;
        }

        @Override
        public JedisSentinelPool getJedisSentinelPool(RedisSentinelResource resource) {
            JedisSentinelPool jedisSentinelPool = map2.get(resource.getUrl());
            if (jedisSentinelPool == null) {
                synchronized (lock) {
                    jedisSentinelPool = map2.get(resource.getUrl());
                    if (jedisSentinelPool == null) {
                        List<RedisSentinelResource.Node> nodes = resource.getNodes();
                        Set<String> sentinels = new HashSet<>();
                        for (RedisSentinelResource.Node node : nodes) {
                            sentinels.add(node.getHost() + ":" + node.getPort());
                        }
                        String password = resource.getPassword();
                        int db = resource.getDb();
                        if (password == null || password.length() == 0) {
                            password = null;
                        }
                        jedisSentinelPool = new JedisSentinelPool(resource.getMaster(), sentinels,
                                poolConfig, timeout, timeout, timeout, resource.getUserName(), password, db,
                                null, timeout, timeout, resource.getSentinelUserName(),
                                resource.getSentinelPassword(), null);
                        map2.put(resource.getUrl(), jedisSentinelPool);
                    }
                }
            }
            return jedisSentinelPool;
        }

        @Override
        public JedisPool getCamelliaJedisPool(CamelliaRedisProxyResource resource) {
            return CamelliaRedisProxyContext.getFactory().initOrGet(resource);
        }

        @Override
        public JedisPool getJedisSentinelSlavesPool(RedisSentinelSlavesResource resource) {
            JedisSentinelSlavesPool jedisSentinelSlavesPool = map3.get(resource.getUrl());
            if (jedisSentinelSlavesPool == null) {
                synchronized (lock) {
                    jedisSentinelSlavesPool = map3.get(resource.getUrl());
                    if (jedisSentinelSlavesPool == null) {
                        jedisSentinelSlavesPool = new JedisSentinelSlavesPool(resource, poolConfig, timeout, redisSentinelSlavesCheckIntervalMillis);
                        map3.put(resource.getUrl(), jedisSentinelSlavesPool);
                    }
                }
            }
            return jedisSentinelSlavesPool;
        }

        @Override
        public JedisPool getRedisProxiesJedisPool(RedisProxiesResource resource) {
            RedisProxyJedisPool redisProxyJedisPool = map4.get(resource.getUrl());
            if (redisProxyJedisPool == null) {
                synchronized (lock) {
                    redisProxyJedisPool = map4.get(resource.getUrl());
                    if (redisProxyJedisPool == null) {
                        List<RedisProxiesResource.Node> nodes = resource.getNodes();
                        List<ServerNode> list = new ArrayList<>();
                        for (RedisProxiesResource.Node node : nodes) {
                            list.add(new ServerNode(node.getHost(), node.getPort()));
                        }
                        DetectedLocalConfCamelliaDiscovery discovery = new DetectedLocalConfCamelliaDiscovery(list);
                        redisProxyJedisPool = new RedisProxyJedisPool.Builder()
                                .timeout(timeout)
                                .poolConfig(poolConfig)
                                .db(resource.getDb())
                                .password(resource.getPassword())
                                .proxyDiscovery(discovery)
                                .build();
                        map4.put(resource.getUrl(), redisProxyJedisPool);
                    }
                }
            }
            return redisProxyJedisPool;
        }

        @Override
        public JedisPool getRedisProxiesDiscoveryJedisPool(RedisProxiesDiscoveryResource resource) {
            return RedisProxiesContext.getFactory().initOrGet(resource);
        }
    }

}
