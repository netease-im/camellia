package com.netease.nim.camellia.redis.proxy.upstream.kv.meta;

/**
 * key type define
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

    private static final KeyType[] array = new KeyType[127];
    static {
        for (byte i = 0; i<127; i++) {
            array[i] = getByValue0(i);
        }
    }

    private static KeyType getByValue0(byte value) {
        for (KeyType keyType : KeyType.values()) {
            if (keyType.value == value) {
                return keyType;
            }
        }
        return null;
    }

    public static KeyType getByValue(byte value) {
        return array[value];
    }
}
