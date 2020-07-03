package com.netease.nim.camellia.redis.proxy.hbase.monitor;

/**
 * 写操作的结果
 * Created by caojiajun on 2020/4/23.
 */
public enum WriteOpeType {

    HBASE_ONLY,//redis没有命中，只需写hbase
    REDIS_HIT,//redis命中，需要同时写redis、hbase
    REDIS_REBUILD_NONE_RESULT,//redis没有命中，需要重建缓存再写，重建时发现hbase也没有
    REDIS_REBUILD_HIT_NULL_CACHE,//redis没有命中，需要重建缓存再写，重建时命中了hbase的null缓存
    REDIS_REBUILD_OK,//redis没有命中，需要重建缓存再写，重建成功
    REDIS_HBASE_ALL,//不检查redis，redis/hbase两边都写一下
    REDIS_REBUILD_DEGRADED,//redis没有命中，需要重建缓存，但是重建操作被降级掉了
    ;
}
