package com.netease.nim.camellia.redis.resource;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.redis.proxy.RedisProxyResource;

/**
 *
 * Created by caojiajun on 2019/11/8.
 */
public enum RedisType {

    //格式：redis://password@127.0.0.1:6379
    //CamelliaRedisTemplate和camellia-redis-proxy同时支持
    Redis("redis://", RedisResource.class),

    //格式：redis-sentinel://password@127.0.0.1:6379,127.0.0.1:6380/masterName
    //CamelliaRedisTemplate和camellia-redis-proxy同时支持
    RedisSentinel("redis-sentinel://", RedisSentinelResource.class),

    //格式：redis-cluster://password@127.0.0.1:6379,127.0.0.1:6380
    //CamelliaRedisTemplate和camellia-redis-proxy同时支持
    RedisCluster("redis-cluster://", RedisClusterResource.class),

    //格式：redis-proxy://id
    //仅CamelliaRedisTemplate支持
    RedisProxy("redis-proxy://", RedisProxyResource.class),//id仅本地生效

    //格式：camellia-redis-proxy://password@proxyName
    //仅CamelliaRedisTemplate支持
    CamelliaRedisProxy("camellia-redis-proxy://", CamelliaRedisProxyResource.class),//proxyName是从注册中心获取的

    //格式：redis-sentinel-slaves://password@127.0.0.1:6379,127.0.0.1:6380/masterName?withMaster=true
    //CamelliaRedisTemplate和camellia-redis-proxy同时支持
    RedisSentinelSlaves("redis-sentinel-slaves://", RedisSentinelSlavesResource.class),

    //格式：redis-cluster-slaves://password@127.0.0.1:6379,127.0.0.1:6380?withMaster=true
    //仅camellia-redis-proxy支持
    RedisClusterSlaves("redis-cluster-slaves://", RedisClusterSlavesResource.class),

    //格式：redis-proxies://password@127.0.0.1:6379,127.0.0.1:6380
    //CamelliaRedisTemplate和camellia-redis-proxy同时支持
    RedisProxies("redis-proxies://", RedisProxiesResource.class),//camellia-redis-proxy会随机挑选一个proxy节点，并当做普通redis去访问

    //格式：redis-proxies-discovery://password@proxyName
    //仅camellia-redis-proxy支持
    RedisProxiesDiscovery("redis-proxies-discovery://", RedisProxiesDiscoveryResource.class),//camellia-redis-proxy会随机挑选一个proxy节点，并当做普通redis去访问

    ;
    private final String prefix;
    private final Class<? extends Resource> clazz;

    RedisType(String prefix, Class<? extends Resource> clazz) {
        this.prefix = prefix;
        this.clazz = clazz;
    }

    public String getPrefix() {
        return prefix;
    }

    public Class<? extends Resource> getClazz() {
        return clazz;
    }
}
