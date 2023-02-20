package com.netease.nim.camellia.redis.base.resource;


/**
 *
 * Created by caojiajun on 2019/11/8.
 */
public enum RedisType {

    //格式：redis://password@127.0.0.1:6379
    //CamelliaRedisTemplate和camellia-redis-proxy同时支持
    Redis("redis://"),

    //格式：redis-sentinel://password@127.0.0.1:6379,127.0.0.1:6380/masterName
    //CamelliaRedisTemplate和camellia-redis-proxy同时支持
    RedisSentinel("redis-sentinel://"),

    //格式：redis-cluster://password@127.0.0.1:6379,127.0.0.1:6380
    //CamelliaRedisTemplate和camellia-redis-proxy同时支持
    RedisCluster("redis-cluster://"),

    //格式：redis-proxy://id
    //仅CamelliaRedisTemplate支持
    RedisProxy("redis-proxy://"),//id仅本地生效

    //格式：camellia-redis-proxy://password@proxyName
    //仅CamelliaRedisTemplate支持
    CamelliaRedisProxy("camellia-redis-proxy://"),//proxyName是从注册中心获取的

    //格式：redis-sentinel-slaves://password@127.0.0.1:6379,127.0.0.1:6380/masterName?withMaster=true
    //CamelliaRedisTemplate和camellia-redis-proxy同时支持
    RedisSentinelSlaves("redis-sentinel-slaves://"),

    //格式：redis-cluster-slaves://password@127.0.0.1:6379,127.0.0.1:6380?withMaster=true
    //仅camellia-redis-proxy支持
    RedisClusterSlaves("redis-cluster-slaves://"),

    //格式：redis-proxies://password@127.0.0.1:6379,127.0.0.1:6380
    //CamelliaRedisTemplate和camellia-redis-proxy同时支持
    RedisProxies("redis-proxies://"),//camellia-redis-proxy会随机挑选一个proxy节点，并当做普通redis去访问

    //格式：redis-proxies-discovery://password@proxyName
    //CamelliaRedisTemplate和camellia-redis-proxy同时支持
    RedisProxiesDiscovery("redis-proxies-discovery://"),//camellia-redis-proxy会随机挑选一个proxy节点，并当做普通redis去访问

    ;
    private final String prefix;

    RedisType(String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }

}
