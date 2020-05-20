package com.netease.nim.camellia.redis.resource;

/**
 *
 * Created by caojiajun on 2019/11/8.
 */
public enum RedisType {

    //格式：redis://password@127.0.0.1:6379
    Redis("redis://"),

    //格式：redis-sentinel://password@127.0.0.1:6379,127.0.0.1:6380/master
    RedisSentinel("redis-sentinel://"),

    //格式：redis-cluster://password@127.0.0.1:6379,127.0.0.1:6380
    RedisCluster("redis-cluster://"),

    //格式：redis-proxy://id
    RedisProxy("redis-proxy://"),//id仅本地生效

    //格式：camellia-redis-proxy://password@proxyName
    CamelliaRedisProxy("camellia-redis-proxy://"),//proxyName是从注册中心获取的

    ;
    private final String prefix;

    RedisType(String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }

}
