package com.netease.nim.camellia.redis.jediscluster;

import com.netease.nim.camellia.redis.conf.CamelliaRedisConstants;
import com.netease.nim.camellia.redis.base.resource.RedisClusterResource;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPoolConfig;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * Created by caojiajun on 2019/7/19.
 */
public interface JedisClusterFactory {

    /**
     * 获取JedisClusterWrapper对象
     * @param redisClusterResource RedisCluster资源定义
     * @return JedisClusterWrapper对象
     */
    JedisClusterWrapper getJedisCluster(RedisClusterResource redisClusterResource);

    /**
     * 一个默认实现
     */
    JedisClusterFactory DEFAULT = new JedisClusterFactory.DefaultJedisClusterFactory();

    /**
     * 一个默认实现
     */
    class DefaultJedisClusterFactory implements JedisClusterFactory {

        private final Object lock = new Object();
        private final ConcurrentHashMap<String, JedisClusterWrapper> map = new ConcurrentHashMap<>();

        private final GenericObjectPoolConfig poolConfig;
        private final int connectionTimeout;
        private final int soTimeout;
        private final int maxAttempts;

        public DefaultJedisClusterFactory() {
            this.poolConfig = new JedisPoolConfig();
            poolConfig.setMinIdle(CamelliaRedisConstants.JedisCluster.minIdle);
            poolConfig.setMaxTotal(CamelliaRedisConstants.JedisCluster.maxTotal);
            poolConfig.setMaxIdle(CamelliaRedisConstants.JedisCluster.maxIdle);
            poolConfig.setMaxWaitMillis(CamelliaRedisConstants.JedisCluster.maxWaitMillis);
            this.connectionTimeout = CamelliaRedisConstants.JedisCluster.connectionTimeout;
            this.soTimeout = CamelliaRedisConstants.JedisCluster.soTimeout;
            this.maxAttempts = CamelliaRedisConstants.JedisCluster.maxAttempts;
        }

        public DefaultJedisClusterFactory(GenericObjectPoolConfig poolConfig, int connectionTimeout, int soTimeout, int maxAttempts) {
            this.poolConfig = poolConfig;
            this.connectionTimeout = connectionTimeout;
            this.soTimeout = soTimeout;
            this.maxAttempts = maxAttempts;
        }

        @Override
        public JedisClusterWrapper getJedisCluster(RedisClusterResource resource) {
            JedisClusterWrapper jedisCluster = map.get(resource.getUrl());
            if (jedisCluster == null) {
                synchronized (lock) {
                    jedisCluster = map.get(resource.getUrl());
                    if (jedisCluster == null) {
                        String password = resource.getPassword();
                        Set<HostAndPort> nodes = new HashSet<>();
                        for (RedisClusterResource.Node node : resource.getNodes()) {
                            HostAndPort hostAndPort = new HostAndPort(node.getHost(), node.getPort());
                            nodes.add(hostAndPort);
                        }
                        if (password == null || password.length() == 0) {
                            password = null;
                        }
                        jedisCluster = new JedisClusterWrapper(nodes, connectionTimeout, soTimeout, maxAttempts, password, poolConfig);
                        map.put(resource.getUrl(), jedisCluster);
                    }
                }
            }
            return jedisCluster;
        }
    }
}
