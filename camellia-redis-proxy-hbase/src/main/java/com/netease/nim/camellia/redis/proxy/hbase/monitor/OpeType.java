package com.netease.nim.camellia.redis.proxy.hbase.monitor;

/**
 *
 * Created by caojiajun on 2020/3/5.
 */
public enum OpeType {
    REDIS_ONLY,
    HIT_TO_HBASE,
    HIT_TO_HBASE_AND_MISS,
    HIT_TO_HBASE_NULL_CACHE,
    ;
}
