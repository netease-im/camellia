package com.netease.nim.camellia.redis.proxy.hbase.conf;

/**
 *
 * Created by caojiajun on 2020/5/11.
 */
public class RedisHBaseProxyHostCounter {
    /**
     * redis-hbase-proxy的实例数量，用于计算分配hbase异步写的队列消费者时分配均衡策略
     */
    public static volatile int redisHBaseProxyInstanceCount = 1;
}
