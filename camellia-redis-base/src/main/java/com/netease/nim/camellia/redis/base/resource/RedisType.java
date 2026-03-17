package com.netease.nim.camellia.redis.base.resource;


import com.netease.nim.camellia.core.model.Resource;

/**
 *
 * Created by caojiajun on 2019/11/8.
 */
public enum RedisType {

    //格式：redis://password@127.0.0.1:6379
    //CamelliaRedisTemplate和camellia-redis-proxy同时支持
    Redis("redis://", false),

    //格式：rediss://password@127.0.0.1:6379
    //CamelliaRedisTemplate和camellia-redis-proxy同时支持（暂不支持CamelliaRedisTemplate）
    Rediss("rediss://", true),

    //格式：redis-sentinel://password@127.0.0.1:6379,127.0.0.1:6380/masterName
    //CamelliaRedisTemplate和camellia-redis-proxy同时支持
    RedisSentinel("redis-sentinel://", false),

    //格式：rediss-sentinel://password@127.0.0.1:6379,127.0.0.1:6380/masterName
    //CamelliaRedisTemplate和camellia-redis-proxy同时支持（暂不支持CamelliaRedisTemplate）
    RedissSentinel("rediss-sentinel://", true),

    //格式：redis-cluster://password@127.0.0.1:6379,127.0.0.1:6380
    //CamelliaRedisTemplate和camellia-redis-proxy同时支持
    RedisCluster("redis-cluster://", false),

    //格式：rediss-cluster://password@127.0.0.1:6379,127.0.0.1:6380
    //CamelliaRedisTemplate和camellia-redis-proxy同时支持（暂不支持CamelliaRedisTemplate）
    RedissCluster("rediss-cluster://", true),

    //格式：redis-proxy://id
    //仅CamelliaRedisTemplate支持
    RedisProxy("redis-proxy://", false),//id仅本地生效

    //格式：camellia-redis-proxy://password@proxyName
    //仅CamelliaRedisTemplate支持
    CamelliaRedisProxy("camellia-redis-proxy://", false),//proxyName是从注册中心获取的

    //格式：redis-sentinel-slaves://password@127.0.0.1:6379,127.0.0.1:6380/masterName?withMaster=true
    //CamelliaRedisTemplate和camellia-redis-proxy同时支持
    RedisSentinelSlaves("redis-sentinel-slaves://", false),

    //格式：rediss-sentinel-slaves://password@127.0.0.1:6379,127.0.0.1:6380/masterName?withMaster=true
    //CamelliaRedisTemplate和camellia-redis-proxy同时支持（暂不支持CamelliaRedisTemplate）
    RedissSentinelSlaves("rediss-sentinel-slaves://", true),

    //格式：redis-cluster-slaves://password@127.0.0.1:6379,127.0.0.1:6380?withMaster=true
    //仅camellia-redis-proxy支持
    RedisClusterSlaves("redis-cluster-slaves://", false),

    //格式：rediss-cluster-slaves://password@127.0.0.1:6379,127.0.0.1:6380?withMaster=true
    //仅camellia-redis-proxy支持
    RedissClusterSlaves("rediss-cluster-slaves://", false),

    //格式：redis-proxies://password@127.0.0.1:6379,127.0.0.1:6380
    //CamelliaRedisTemplate和camellia-redis-proxy同时支持
    RedisProxies("redis-proxies://", false),//camellia-redis-proxy会随机挑选一个proxy节点，并当做普通redis去访问

    //格式：rediss-proxies://password@127.0.0.1:6379,127.0.0.1:6380
    //CamelliaRedisTemplate和camellia-redis-proxy同时支持（暂不支持CamelliaRedisTemplate）
    RedissProxies("rediss-proxies://", true),//camellia-redis-proxy会随机挑选一个proxy节点，并当做普通redis去访问

    //格式：redis-proxies-discovery://password@proxyName
    //CamelliaRedisTemplate和camellia-redis-proxy同时支持
    RedisProxiesDiscovery("redis-proxies-discovery://", false),//camellia-redis-proxy会随机挑选一个proxy节点，并当做普通redis去访问

    //格式：rediss-proxies-discovery://password@proxyName
    //CamelliaRedisTemplate和camellia-redis-proxy同时支持（暂不支持CamelliaRedisTemplate）
    RedissProxiesDiscovery("rediss-proxies-discovery://", true),//camellia-redis-proxy会随机挑选一个proxy节点，并当做普通redis去访问

    //格式：sentinel://username:passwd@host:port,host:port
    //camellia-redis-proxy内部使用
    Sentinel("sentinel://", false),

    //格式：ssentinel://username:passwd@host:port,host:port
    //camellia-redis-proxy内部使用
    SSentinel("ssentinel://", true),

    //格式：redis-uds://password@path?db=1
    //仅camellia-redis-proxy支持
    UnixDomainSocket("redis-uds://", false),

    //格式：redis-kv://namespace
    //仅camellia-redis-proxy支持
    RedisKV("redis-kv://", false),

    ;
    private final String prefix;
    private final boolean tlsEnable;

    RedisType(String prefix, boolean tlsEnable) {
        this.prefix = prefix;
        this.tlsEnable = tlsEnable;
    }

    public String getPrefix() {
        return prefix;
    }

    public boolean isTlsEnable() {
        return tlsEnable;
    }

    public static RedisType parseRedisType(Resource resource) {
        for (RedisType type : RedisType.values()) {
            if (resource.getUrl().startsWith(type.getPrefix())) {
                return type;
            }
        }
        return null;
    }

}
