package com.netease.nim.camellia.redis.eureka.springboot;

import com.netease.nim.camellia.redis.eureka.base.EurekaProxyDiscovery;
import com.netease.nim.camellia.redis.proxy.CamelliaRedisProxyFactory;
import com.netease.nim.camellia.redis.proxy.Proxy;
import com.netease.nim.camellia.redis.proxy.ProxyDiscovery;
import com.netease.nim.camellia.redis.proxy.RedisProxyJedisPool;
import com.netease.nim.camellia.redis.resource.CamelliaRedisProxyResource;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * Created by caojiajun on 2020/4/23.
 */
public class CamelliaRedisProxyEurekaFactory implements CamelliaRedisProxyFactory {

    private final DiscoveryClient discoveryClient;
    private final CamelliaRedisEurekaProperties properties;

    public CamelliaRedisProxyEurekaFactory(DiscoveryClient discoveryClient, CamelliaRedisEurekaProperties properties) {
        this.discoveryClient = discoveryClient;
        this.properties = properties;
    }

    private final ConcurrentHashMap<String, JedisPool> poolMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ProxyDiscovery> proxyDiscoveryMap = new ConcurrentHashMap<>();

    @Override
    public JedisPool initOrGet(CamelliaRedisProxyResource resource) {
        JedisPool jedisPool = poolMap.get(resource.getUrl());
        if (jedisPool == null) {
            synchronized (CamelliaRedisProxyEurekaFactory.class) {
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
                    jedisPool = new RedisProxyJedisPool(proxyDiscovery, poolConfig, timeout, password, properties.isSideCarFirst());
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
                    proxyDiscovery = new EurekaProxyDiscovery(discoveryClient, proxyName, properties.getRefreshIntervalSeconds());
                    proxyDiscoveryMap.putIfAbsent(proxyName, proxyDiscovery);
                }
            }
        }
        return proxyDiscovery;
    }

    private GenericObjectPoolConfig poolConfig() {
        GenericObjectPoolConfig poolConfig = new JedisPoolConfig();
        CamelliaRedisEurekaProperties.RedisConf redisConf = properties.getRedisConf();
        poolConfig.setMaxIdle(redisConf.getMaxIdle());
        poolConfig.setMinIdle(redisConf.getMinIdle());
        poolConfig.setMaxTotal(redisConf.getMaxActive());
        poolConfig.setMaxWaitMillis(redisConf.getMaxWaitMillis());
        return poolConfig;
    }

    private int timeout() {
        CamelliaRedisEurekaProperties.RedisConf redisConf = properties.getRedisConf();
        return redisConf.getTimeout();
    }
}
