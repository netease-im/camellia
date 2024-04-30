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
    version_6((byte) 6),
    version_7((byte) 7),
    version_8((byte) 8),
    version_9((byte) 9),
    version_10((byte) 10),
    ;

    private final byte value;

    EncodeVersion(byte value) {
        this.value = value;
    }

    public final byte getValue() {
        return value;
    }

    private static final EncodeVersion[] array = new EncodeVersion[127];
    static {
        for (byte i = 0; i<127; i++) {
            array[i] = getByValue0(i);
        }
    }

    private static EncodeVersion getByValue0(byte value) {
        for (EncodeVersion version : EncodeVersion.values()) {
            if (version.value == value) {
                return version;
            }
        }
        return null;
    }

    public static EncodeVersion getByValue(byte value) {
        return array[value];
    }
}
