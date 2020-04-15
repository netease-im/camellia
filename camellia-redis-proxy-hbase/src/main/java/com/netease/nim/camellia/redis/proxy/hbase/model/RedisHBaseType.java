package com.netease.nim.camellia.redis.proxy.hbase.model;

import redis.clients.util.SafeEncoder;

/**
 *
 * Created by caojiajun on 2020/2/22.
 */
public enum RedisHBaseType {

    STRING(SafeEncoder.encode("string")),
    HASH(SafeEncoder.encode("hash")),
    LIST(SafeEncoder.encode("list")),
    SET(SafeEncoder.encode("set")),
    ZSET(SafeEncoder.encode("zset")),

    INNER(SafeEncoder.encode("inner")),
    ;

    private byte[] raw;

    RedisHBaseType(byte[] raw) {
        this.raw = raw;
    }

    public byte[] raw() {
        return raw;
    }

    public static RedisHBaseType byName(String name) {
        if (name == null || name.length() == 0) return null;
        if (name.equalsIgnoreCase("none")) return null;
        for (RedisHBaseType redisHBaseType : RedisHBaseType.values()) {
            if (redisHBaseType.name().equalsIgnoreCase(name)) {
                return redisHBaseType;
            }
        }
        return null;
    }
}
