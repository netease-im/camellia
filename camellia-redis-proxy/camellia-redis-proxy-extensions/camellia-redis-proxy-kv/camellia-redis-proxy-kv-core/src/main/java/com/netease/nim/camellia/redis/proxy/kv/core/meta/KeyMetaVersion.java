package com.netease.nim.camellia.redis.proxy.kv.core.meta;

/**
 * Created by caojiajun on 2024/4/11
 */
public enum KeyMetaVersion {

    version_0((byte) 0),
    version_1((byte) 1),
    ;

    private final byte value;

    KeyMetaVersion(byte value) {
        this.value = value;
    }

    public final byte getValue() {
        return value;
    }

    public static KeyMetaVersion getByValue(byte value) {
        if (value == (byte) 0) {
            return KeyMetaVersion.version_0;
        } else if (value == (byte) 1) {
            return KeyMetaVersion.version_1;
        }
        return null;
    }
}
