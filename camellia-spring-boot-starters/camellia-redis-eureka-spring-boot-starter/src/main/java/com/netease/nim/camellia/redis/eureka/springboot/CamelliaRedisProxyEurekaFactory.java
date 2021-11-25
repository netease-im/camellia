package com.netease.nim.camellia.redis.eureka.springboot;

import com.netease.nim.camellia.redis.proxy.*;
import com.netease.nim.camellia.redis.proxy.ProxyDiscoveryFactory;
import com.netease.nim.camellia.redis.proxy.discovery.common.*;
import com.netease.nim.camellia.redis.proxy.discovery.jedis.ProxyJedisPoolConfig;
import com.netease.nim.camellia.redis.proxy.discovery.jedis.RedisProxyJedisPool;
import com.netease.nim.camellia.redis.resource.CamelliaRedisProxyResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPool;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * Created by caojiajun on 2020/4/23.
 */
public class CamelliaRedisProxyEurekaFactory implements CamelliaRedisProxyFactory {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaRedisProxyEurekaFactory.class);

    private final ProxyJedisPoolConfig proxyJedisPoolConfig;
    private final ProxyDiscoveryFactory proxyDiscoveryFactory;

    public CamelliaRedisProxyEurekaFactory(ProxyJedisPoolConfig proxyJedisPoolConfig, ProxyDiscoveryFactory proxyDiscoveryFactory) {
        this.proxyJedisPoolConfig = proxyJedisPoolConfig;
        this.proxyDiscoveryFactory = proxyDiscoveryFactory;
    }

    private final ConcurrentHashMap<String, JedisPool> poolMap = new ConcurrentHashMap<>();

    @Override
    public JedisPool initOrGet(CamelliaRedisProxyResource resource) {
        JedisPool jedisPool = poolMap.get(resource.getUrl());
        if (jedisPool == null) {
            synchronized (poolMap) {
                jedisPool = poolMap.get(resource.getUrl());
                if (jedisPool == null) {
                    String proxyName = resource.getProxyName();
                    String password = resource.getPassword();
                    ProxyDiscovery proxyDiscovery = proxyDiscoveryFactory.getProxyDiscovery(proxyName);
                    List<Proxy> proxyList = proxyDiscovery.findAll();
                    if (proxyList == null || proxyList.isEmpty()) {
                        throw new IllegalArgumentException("proxyList is empty, proxyName=" + proxyName);
                    }
                    jedisPool = new RedisProxyJedisPool.Builder()
                            .proxyDiscovery(proxyDiscovery)
                            .bid(resource.getBid())
                            .bgroup(resource.getBgroup())
                            .poolConfig(proxyJedisPoolConfig.getJedisPoolConfig())
                            .timeout(proxyJedisPoolConfig.getTimeout())
                            .password(password)
                            .sideCarFirst(proxyJedisPoolConfig.isSideCarFirst())
                            .regionResolver(new RegionResolver.IpSegmentRegionResolver(proxyJedisPoolConfig.getRegionResolveConf(), proxyJedisPoolConfig.getDefaultRegion()))
                            .jedisPoolLazyInit(proxyJedisPoolConfig.isJedisPoolLazyInit())
                            .jedisPoolInitialSize(proxyJedisPoolConfig.getJedisPoolInitialSize())
                            .build();
                    logger.info("RedisProxyJedisPool init success, resource = {}, proxyJedisPoolConfig = {}", resource.getUrl(), proxyJedisPoolConfig);
                    poolMap.put(resource.getUrl(), jedisPool);
                }
            }
        }
        return jedisPool;
    }
}
