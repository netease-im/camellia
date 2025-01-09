package com.netease.nim.camellia.redis.proxy.upstream.local.storage.enums;


/**
 * Created by caojiajun on 2024/12/27
 */
public enum DataType {

    string((byte) 1),
    hash((byte) 2),
    zset((byte) 3),
    list((byte) 4),
    set((byte) 5),
    ;

    private final byte value;

    DataType(byte value) {
        this.value = value;
    }

    public byte getValue() {
        return value;
    }

    private static final DataType[] array = new DataType[127];
    static {
        for (byte i = 0; i<127; i++) {
            array[i] = getByValue0(i);
        }
    }

    private static DataType getByValue0(byte value) {
        for (DataType keyType : DataType.values()) {
            if (keyType.value == value) {
                return keyType;
            }
        }
        return null;
    }

    public static DataType getByValue(byte value) {
        return array[value];
    }
}
