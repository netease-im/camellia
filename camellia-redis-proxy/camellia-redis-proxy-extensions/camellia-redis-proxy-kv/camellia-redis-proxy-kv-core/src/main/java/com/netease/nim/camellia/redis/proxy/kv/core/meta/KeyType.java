package com.netease.nim.camellia.redis.proxy.kv.core.meta;

/**
 * Created by caojiajun on 2024/4/7
 */
public enum KeyType {

    string((byte) 1),
    hash((byte) 2),
    zset((byte) 3),
    list((byte) 4),
    set((byte) 5),
    ;

    private final byte value;

    KeyType(byte value) {
        this.value = value;
    }

    public byte getValue() {
        return value;
    }

    public static KeyType getByValue(byte value) {
        for (KeyType keyType : KeyType.values()) {
            if (keyType.value == value) {
                return keyType;
            }
        }
        return null;
    }
}
