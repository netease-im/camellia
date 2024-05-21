package com.netease.nim.camellia.redis.proxy.upstream.kv.cache;

/**
 * Created by caojiajun on 2024/5/21
 */
public class ValueWrapper {
    private final byte[] value;

    public ValueWrapper(byte[] value) {
        this.value = value;
    }

    public byte[] getValue() {
        return value;
    }
}
