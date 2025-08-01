package com.netease.nim.camellia.redis.jediscluster;

import com.netease.nim.camellia.redis.base.resource.RedisClusterSlavesResource;
import com.netease.nim.camellia.redis.conf.CamelliaRedisConstants;
import com.netease.nim.camellia.redis.base.resource.RedisClusterResource;
import com.netease.nim.camellia.tools.executor.CamelliaThreadFactory;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisPoolConfig;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

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
     * 获取JedisClusterWrapper对象
     * @param redisClusterSlavesResource RedisCluster资源定义
     * @return JedisClusterWrapper对象
     */
    JedisClusterWrapper getJedisCluster(RedisClusterSlavesResource redisClusterSlavesResource);

    /**
     * 一个默认实现
     */
    JedisClusterFactory DEFAULT = new JedisClusterFactory.DefaultJedisClusterFactory();

    /**
     * 一个默认实现
     */
    class DefaultJedisClusterFactory implements JedisClusterFactory {

        private final Object lock1 = new Object();
        private final Object lock2 = new Object();
        private final ConcurrentHashMap<String, JedisClusterWrapper> map1 = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, JedisClusterWrapper> map2 = new ConcurrentHashMap<>();

        private final GenericObjectPoolConfig poolConfig;
        private final int connectionTimeout;
        private final int soTimeout;
        private final int maxAttempts;

        private final int redisClusterSlaveRenewIntervalSeconds;
        private final ScheduledExecutorService scheduledExecutorService;

        public DefaultJedisClusterFactory() {
            this.poolConfig = new JedisPoolConfig();
            poolConfig.setMinIdle(CamelliaRedisConstants.JedisCluster.minIdle);
            poolConfig.setMaxTotal(CamelliaRedisConstants.JedisCluster.maxTotal);
            poolConfig.setMaxIdle(CamelliaRedisConstants.JedisCluster.maxIdle);
            poolConfig.setMaxWaitMillis(CamelliaRedisConstants.JedisCluster.maxWaitMillis);
            this.connectionTimeout = CamelliaRedisConstants.JedisCluster.connectionTimeout;
            this.soTimeout = CamelliaRedisConstants.JedisCluster.soTimeout;
            this.maxAttempts = CamelliaRedisConstants.JedisCluster.maxAttempts;
            this.redisClusterSlaveRenewIntervalSeconds = CamelliaRedisConstants.JedisCluster.redisClusterSlaveRenewIntervalSeconds;
            this.scheduledExecutorService = new ScheduledThreadPoolExecutor(2, new CamelliaThreadFactory("redis-cluster-slaves"));
        }

        public DefaultJedisClusterFactory(int redisClusterSlaveRenewIntervalSeconds, ScheduledExecutorService scheduledExecutorService) {
            this.poolConfig = new JedisPoolConfig();
            poolConfig.setMinIdle(CamelliaRedisConstants.JedisCluster.minIdle);
            poolConfig.setMaxTotal(CamelliaRedisConstants.JedisCluster.maxTotal);
            poolConfig.setMaxIdle(CamelliaRedisConstants.JedisCluster.maxIdle);
            poolConfig.setMaxWaitMillis(CamelliaRedisConstants.JedisCluster.maxWaitMillis);
            this.connectionTimeout = CamelliaRedisConstants.JedisCluster.connectionTimeout;
            this.soTimeout = CamelliaRedisConstants.JedisCluster.soTimeout;
            this.maxAttempts = CamelliaRedisConstants.JedisCluster.maxAttempts;
            this.redisClusterSlaveRenewIntervalSeconds = redisClusterSlaveRenewIntervalSeconds;
            this.scheduledExecutorService = scheduledExecutorService;
        }

        public DefaultJedisClusterFactory(GenericObjectPoolConfig poolConfig, int connectionTimeout, int soTimeout, int maxAttempts, int redisClusterSlaveRenewIntervalSeconds) {
            this.poolConfig = poolConfig;
            this.connectionTimeout = connectionTimeout;
            this.soTimeout = soTimeout;
            this.maxAttempts = maxAttempts;
            this.redisClusterSlaveRenewIntervalSeconds = redisClusterSlaveRenewIntervalSeconds;
            this.scheduledExecutorService = new ScheduledThreadPoolExecutor(2, new CamelliaThreadFactory("redis-cluster-slaves"));
        }

        public DefaultJedisClusterFactory(GenericObjectPoolConfig poolConfig, int connectionTimeout, int soTimeout, int maxAttempts) {
            this.poolConfig = poolConfig;
            this.connectionTimeout = connectionTimeout;
            this.soTimeout = soTimeout;
            this.maxAttempts = maxAttempts;
            this.redisClusterSlaveRenewIntervalSeconds = CamelliaRedisConstants.JedisCluster.redisClusterSlaveRenewIntervalSeconds;
            this.scheduledExecutorService = new ScheduledThreadPoolExecutor(2, new CamelliaThreadFactory("redis-cluster-slaves"));
        }

        public DefaultJedisClusterFactory(GenericObjectPoolConfig poolConfig, int connectionTimeout, int soTimeout, int maxAttempts,
                                          int redisClusterSlaveRenewIntervalSeconds, ScheduledExecutorService scheduledExecutorService) {
            this.poolConfig = poolConfig;
            this.connectionTimeout = connectionTimeout;
            this.soTimeout = soTimeout;
            this.maxAttempts = maxAttempts;
            this.redisClusterSlaveRenewIntervalSeconds = redisClusterSlaveRenewIntervalSeconds;
            this.scheduledExecutorService = scheduledExecutorService;
        }

        @Override
        public JedisClusterWrapper getJedisCluster(RedisClusterResource resource) {
            String key = resource.getUrl();
            JedisClusterWrapper jedisCluster = map1.get(key);
            if (jedisCluster == null) {
                synchronized (lock1) {
                    jedisCluster = map1.get(key);
                    if (jedisCluster == null) {
                        String password = resource.getPassword();
                        Set<HostAndPort> nodes = new HashSet<>();
                        for (RedisClusterResource.Node node : resource.getNodes()) {
                            HostAndPort hostAndPort = new HostAndPort(node.getHost(), node.getPort());
                            nodes.add(hostAndPort);
                        }
                        if (password == null || password.isEmpty()) {
                            password = null;
                        }
                        DefaultJedisClientConfig.Builder builder = DefaultJedisClientConfig.builder()
                                .socketTimeoutMillis(soTimeout)
                                .connectionTimeoutMillis(connectionTimeout)
                                .password(password);
                        DefaultJedisClientConfig jedisClientConfig = builder.build();
                        jedisCluster = new JedisClusterWrapper(nodes, jedisClientConfig, maxAttempts, poolConfig);
                        map1.put(key, jedisCluster);
                    }
                }
            }
            return jedisCluster;
        }

        @Override
        public JedisClusterWrapper getJedisCluster(RedisClusterSlavesResource resource) {
            String key = resource.getUrl();
            JedisClusterWrapper jedisCluster = map1.get(key);
            if (jedisCluster == null) {
                synchronized (lock1) {
                    jedisCluster = map1.get(key);
                    if (jedisCluster == null) {
                        String password = resource.getPassword();
                        Set<HostAndPort> nodes = new HashSet<>();
                        for (RedisClusterResource.Node node : resource.getNodes()) {
                            HostAndPort hostAndPort = new HostAndPort(node.getHost(), node.getPort());
                            nodes.add(hostAndPort);
                        }
                        if (password == null || password.isEmpty()) {
                            password = null;
                        }
                        DefaultJedisClientConfig.Builder builder = DefaultJedisClientConfig.builder()
                                .socketTimeoutMillis(soTimeout)
                                .connectionTimeoutMillis(connectionTimeout)
                                .password(password)
                                .readOnlyForRedisClusterReplicas();
                        DefaultJedisClientConfig jedisClientConfig = builder.build();
                        jedisCluster = new JedisClusterWrapper(nodes, jedisClientConfig, maxAttempts, poolConfig);
                        map1.put(key, jedisCluster);
                    }
                }
            }
            return jedisCluster;
        }
    }

}
