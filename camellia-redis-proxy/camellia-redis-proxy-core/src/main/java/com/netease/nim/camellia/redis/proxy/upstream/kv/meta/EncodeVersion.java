package com.netease.nim.camellia.redis.proxy.upstream.kv.meta;

/**
 * Created by caojiajun on 2024/4/11
 */
public enum EncodeVersion {

    version_0((byte) 0),
    version_1((byte) 1),
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
