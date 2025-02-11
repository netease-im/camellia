package com.netease.nim.camellia.redis.proxy.upstream.local.storage.wal;

/**
 * Created by caojiajun on 2025/2/11
 */
public enum WalMode {
    sync,
    async,
    ;

    public static WalMode byString(String value) {
        for (WalMode walMode : WalMode.values()) {
            if (walMode.name().equalsIgnoreCase(value)) {
                return walMode;
            }
        }
        return null;
    }
}
