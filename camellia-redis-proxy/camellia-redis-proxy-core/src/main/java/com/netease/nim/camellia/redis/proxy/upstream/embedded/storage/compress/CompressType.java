package com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.compress;

/**
 * Created by caojiajun on 2025/1/2
 */
public enum CompressType {

    none((byte) 0),
    zstd((byte) 1),
    ;
    private final byte type;

    CompressType(byte type) {
        this.type = type;
    }

    public byte getType() {
        return type;
    }

    public static CompressType getByValue(byte type) {
        for (CompressType compressType : CompressType.values()) {
            if (compressType.type == type) {
                return compressType;
            }
        }
        return CompressType.none;
    }
}
