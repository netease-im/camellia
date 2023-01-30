package com.netease.nim.camellia.redis.pipeline;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.redis.base.exception.CamelliaRedisException;
import com.netease.nim.camellia.redis.base.resource.*;
import com.netease.nim.camellia.redis.jedis.JedisPoolFactory;
import com.netease.nim.camellia.redis.jediscluster.JedisClusterFactory;
import com.netease.nim.camellia.redis.jediscluster.JedisClusterWrapper;
import com.netease.nim.camellia.redis.proxy.CamelliaRedisProxyContext;
import com.netease.nim.camellia.redis.proxy.RedisProxyResource;
import com.netease.nim.camellia.redis.util.CloseUtil;
import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.JedisClusterException;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisMovedDataException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用于pipeline
 * Created by caojiajun on 2019/7/22.
 */
public interface RedisClientPool {

    /**
     * 获取一个client
     * @param resource 资源地址
     * @param key 操作的key
     * @return client
     */
    Client getClient(Resource resource, byte[] key);

    /**
     * 根据资源地址和host/port获取Client对象
     * @param resource 资源地址，只能是RedisClusterResource
     * @param host host
     * @param port port
     * @return client
     */
    Client getClient(Resource resource, String host, int port);

    /**
     * clear RedisClientPool，释放相关资源，使用完之后必须调用，
     * 调用clear之后可以复用本对象
     */
    void clear();

    /**
     * 处理异常
     * @param resource 资源地址
     * @param e 异常
     */
    void handlerException(Resource resource, Exception e);

    /**
     * 一个默认实现
     */
    class DefaultRedisClientPool implements RedisClientPool {

        private final Map<String, Jedis> jedisMap = new ConcurrentHashMap<>();
        private final Map<JedisPool, Jedis> jedisClusterMap = new ConcurrentHashMap<>();

        private final JedisPoolFactory jedisPoolFactory;
        private final JedisClusterFactory jedisClusterFactory;

        public DefaultRedisClientPool(JedisPoolFactory jedisPoolFactory, JedisClusterFactory jedisClusterFactory) {
            this.jedisPoolFactory = jedisPoolFactory;
            this.jedisClusterFactory = jedisClusterFactory;
        }

        @Override
        public Client getClient(Resource resource, byte[] key) {

            try {
                if (resource instanceof RedisResource) {
                    Jedis jedis1 = jedisMap.get(resource.getUrl());
                    if (jedis1 != null) {
                        return jedis1.getClient();
                    }
                    jedis1 = jedisPoolFactory.getJedisPool((RedisResource) resource).getResource();
                    jedisMap.put(resource.getUrl(), jedis1);
                    return jedis1.getClient();
                } else if (resource instanceof RedisClusterResource) {
                    JedisClusterWrapper jedisCluster = jedisClusterFactory.getJedisCluster((RedisClusterResource) resource);
                    JedisPool jedisPool = jedisCluster.getJedisPool(key);
                    Jedis jedis2 = jedisClusterMap.get(jedisPool);
                    if (jedis2 != null) {
                        return jedis2.getClient();
                    }
                    jedis2 = jedisPool.getResource();
                    jedisClusterMap.put(jedisPool, jedis2);
                    return jedis2.getClient();
                } else if (resource instanceof RedisSentinelResource) {
                    Jedis jedis3 = jedisMap.get(resource.getUrl());
                    if (jedis3 != null) {
                        return jedis3.getClient();
                    }
                    jedis3 = jedisPoolFactory.getJedisSentinelPool((RedisSentinelResource) resource).getResource();
                    jedisMap.put(resource.getUrl(), jedis3);
                    return jedis3.getClient();
                } else if (resource instanceof RedisProxyResource) {
                    Jedis jedis4 = jedisMap.get(resource.getUrl());
                    if (jedis4 != null) {
                        return jedis4.getClient();
                    }
                    jedis4 = ((RedisProxyResource) resource).getJedisPool().getResource();
                    jedisMap.put(resource.getUrl(), jedis4);
                    return jedis4.getClient();
                } else if (resource instanceof CamelliaRedisProxyResource) {
                    Jedis jedis5 = jedisMap.get(resource.getUrl());
                    if (jedis5 != null) {
                        return jedis5.getClient();
                    }
                    JedisPool jedisPool = CamelliaRedisProxyContext.getFactory().initOrGet((CamelliaRedisProxyResource) resource);
                    jedis5 = jedisPool.getResource();
                    jedisMap.put(resource.getUrl(), jedis5);
                    return jedis5.getClient();
                } else if (resource instanceof RedisSentinelSlavesResource) {
                    Jedis jedis6 = jedisMap.get(resource.getUrl());
                    if (jedis6 != null) {
                        return jedis6.getClient();
                    }
                    jedis6 = jedisPoolFactory.getJedisSentinelSlavesPool((RedisSentinelSlavesResource) resource).getResource();
                    jedisMap.put(resource.getUrl(), jedis6);
                    return jedis6.getClient();
                } else if (resource instanceof RedisProxiesResource) {
                    Jedis jedis6 = jedisMap.get(resource.getUrl());
                    if (jedis6 != null) {
                        return jedis6.getClient();
                    }
                    jedis6 = jedisPoolFactory.getRedisProxiesJedisPool((RedisProxiesResource) resource).getResource();
                    jedisMap.put(resource.getUrl(), jedis6);
                    return jedis6.getClient();
                }
                throw new UnsupportedOperationException();
            } catch (Exception e) {
                handlerException(resource, e);
                throw e;
            }
        }

        @Override
        public Client getClient(Resource resource, String host, int port) {
            try {
                if (resource instanceof RedisClusterResource) {
                    JedisClusterWrapper jedisCluster = jedisClusterFactory.getJedisCluster((RedisClusterResource) resource);
                    JedisPool jedisPool = jedisCluster.getJedisPool(host, port);
                    Jedis jedis = jedisClusterMap.get(jedisPool);
                    if (jedis != null) {
                        return jedis.getClient();
                    }
                    jedis = jedisPool.getResource();
                    jedisClusterMap.put(jedisPool, jedis);
                    return jedis.getClient();
                }
                throw new CamelliaRedisException("only support RedisClusterResource");
            } catch (Exception e) {
                handlerException(resource, e);
                throw e;
            }
        }

        @Override
        public void clear() {
            if (!jedisMap.isEmpty()) {
                for (Map.Entry<String, Jedis> entry : jedisMap.entrySet()) {
                    CloseUtil.closeQuietly(entry.getValue());
                }
                jedisMap.clear();
            }
            if (!jedisClusterMap.isEmpty()) {
                for (Map.Entry<JedisPool, Jedis> entry : jedisClusterMap.entrySet()) {
                    CloseUtil.closeQuietly(entry.getValue());
                }
                jedisClusterMap.clear();
            }
        }

        @Override
        public void handlerException(Resource resource, Exception e) {
            if (resource instanceof RedisClusterResource) {
                if (e instanceof JedisMovedDataException || e instanceof JedisConnectionException || e instanceof JedisClusterException) {
                    JedisClusterWrapper jedisCluster = jedisClusterFactory.getJedisCluster((RedisClusterResource) resource);
                    jedisCluster.renewSlotCache();
                }
            }
        }
    }
}
