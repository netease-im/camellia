package com.netease.nim.camellia.redis.proxy.hbase.monitor;

/**
 * 读操作的结果
 * Created by caojiajun on 2020/3/5.
 */
public enum ReadOpeType {

    REDIS_ONLY,//redis命中，直接返回
    HIT_TO_HBASE,//redis没有命中，穿透到hbase，然后返回
    HIT_TO_HBASE_AND_MISS,//redis没有命中，穿透到hbase，发现hbase也没有，然后返回
    HIT_TO_HBASE_NULL_CACHE,//redis没有命中，但是命中了hbase的null缓存，然后返回
    ;
}
