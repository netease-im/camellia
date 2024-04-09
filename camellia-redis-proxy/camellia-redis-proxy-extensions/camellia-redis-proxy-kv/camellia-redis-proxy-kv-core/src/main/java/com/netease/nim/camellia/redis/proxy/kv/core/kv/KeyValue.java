package com.netease.nim.camellia.redis.proxy.kv.core.kv;

/**
 * Created by caojiajun on 2024/4/7
 */
public class KeyValue {

    private final byte[] key;
    private final byte[] value;

    public KeyValue(byte[] key, byte[] value) {
        this.key = key;
        this.value = value;
    }

    public byte[] getKey() {
        return key;
    }

    public byte[] getValue() {
        return value;
    }
}
