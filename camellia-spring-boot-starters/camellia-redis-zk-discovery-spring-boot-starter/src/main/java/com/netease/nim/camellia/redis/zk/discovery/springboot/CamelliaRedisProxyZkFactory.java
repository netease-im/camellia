package com.netease.nim.camellia.redis.zk.discovery.springboot;

import com.netease.nim.camellia.redis.proxy.CamelliaRedisProxyFactory;
import com.netease.nim.camellia.redis.proxy.Proxy;
import com.netease.nim.camellia.redis.proxy.ProxyDiscovery;
import com.netease.nim.camellia.redis.proxy.RedisProxyJedisPool;
import com.netease.nim.camellia.redis.resource.CamelliaRedisProxyResource;
import com.netease.nim.camellia.redis.zk.discovery.ZkClientFactory;
import com.netease.nim.camellia.redis.zk.discovery.ZkProxyDiscovery;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.JedisPool;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * Created by caojiajun on 2020/8/12
 */
public class CamelliaRedisProxyZkFactory implements CamelliaRedisProxyFactory {

    private final ConcurrentHashMap<String, JedisPool> poolMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ProxyDiscovery> proxyDiscoveryMap = new ConcurrentHashMap<>();

    private final CamelliaRedisZkDiscoveryProperties properties;
    private final ZkClientFactory factory;

    public CamelliaRedisProxyZkFactory(CamelliaRedisZkDiscoveryProperties properties) {
        this.properties = properties;
        this.factory = new ZkClientFactory(properties.getSessionTimeoutMs(), properties.getConnectionTimeoutMs(),
                properties.getBaseSleepTimeMs(), properties.getMaxRetries());
    }

    @Override
    public JedisPool initOrGet(CamelliaRedisProxyResource resource) {
        JedisPool jedisPool = poolMap.get(resource.getUrl());
        if (jedisPool == null) {
            synchronized (CamelliaRedisProxyZkFactory.class) {
                jedisPool = poolMap.get(resource.getUrl());
                if (jedisPool == null) {
                    String proxyName = resource.getProxyName();
                    String password = resource.getPassword();
                    ProxyDiscovery proxyDiscovery = getProxyDiscovery(proxyName);
                    List<Proxy> proxyList = proxyDiscovery.findAll();
                    if (proxyList == null || proxyList.isEmpty()) {
                        throw new IllegalArgumentException("proxyList is empty, proxyName=" + proxyName);
                    }
                    GenericObjectPoolConfig poolConfig = poolConfig();
                    int timeout = timeout();
                    jedisPool = new RedisProxyJedisPool(proxyDiscovery, poolConfig, timeout, password, properties.isSidCarFirst());
                    poolMap.put(resource.getUrl(), jedisPool);
                }
            }
        }
        return jedisPool;
    }

    private ProxyDiscovery getProxyDiscovery(String proxyName) {
        ProxyDiscovery proxyDiscovery = proxyDiscoveryMap.get(proxyName);
        if (proxyDiscovery == null) {
            synchronized (ProxyDiscovery.class) {
                proxyDiscovery = proxyDiscoveryMap.get(proxyName);
                if (proxyDiscovery == null) {
                    proxyDiscovery = new ZkProxyDiscovery(factory, properties.getZkUrl(),
                            properties.getBasePath(), proxyName, properties.getReloadIntervalSeconds());
                    proxyDiscoveryMap.putIfAbsent(proxyName, proxyDiscovery);
                }
            }
        }
        return proxyDiscovery;
    }

    private GenericObjectPoolConfig poolConfig() {
        GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
        CamelliaRedisZkDiscoveryProperties.RedisConf redisConf = properties.getRedisConf();
        poolConfig.setMaxIdle(redisConf.getMaxIdle());
        poolConfig.setMinIdle(redisConf.getMinIdle());
        poolConfig.setMaxTotal(redisConf.getMaxActive());
        poolConfig.setMaxWaitMillis(redisConf.getMaxWaitMillis());
        return poolConfig;
    }

    private int timeout() {
        CamelliaRedisZkDiscoveryProperties.RedisConf redisConf = properties.getRedisConf();
        return redisConf.getTimeout();
    }
}
