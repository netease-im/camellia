package com.netease.nim.camellia.redis.proxy.upstream.local.storage.wal;

/**
 * Created by caojiajun on 2025/1/16
 */
public enum WalEntryType {
    string((byte) 1),
    ;

    private final byte type;

    WalEntryType(byte type) {
        this.type = type;
    }

    public byte getType() {
        return type;
    }
}
