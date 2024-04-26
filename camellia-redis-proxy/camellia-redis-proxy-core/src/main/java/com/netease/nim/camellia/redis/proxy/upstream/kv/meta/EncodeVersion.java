package com.netease.nim.camellia.redis.proxy.upstream.kv.meta;

/**
 * string
 * v0 只有key-meta
 * <p>
 * hash
 * v0 无缓存，且key-meta里有field-count
 * v1 无缓存，且key-meta里没有field-count
 * v2 v0结构，且有hgetall+hget缓存
 * v3 v1结构，且有hgetall+hget缓存
 * <p>
 * Created by caojiajun on 2024/4/11
 */
public enum EncodeVersion {

    version_0((byte) 0),
    version_1((byte) 1),
    version_2((byte) 2),
    version_3((byte) 3),
    version_4((byte) 4),
    version_5((byte) 5),
    ;

    private final byte value;

    EncodeVersion(byte value) {
        this.value = value;
    }

    public final byte getValue() {
        return value;
    }

    public static EncodeVersion getByValue(byte value) {
        if (value == (byte) 0) {
            return EncodeVersion.version_0;
        } else if (value == (byte) 1) {
            return EncodeVersion.version_1;
        }
        return null;
    }
}
