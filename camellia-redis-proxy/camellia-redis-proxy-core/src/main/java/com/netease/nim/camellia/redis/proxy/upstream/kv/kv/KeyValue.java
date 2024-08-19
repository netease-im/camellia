package com.netease.nim.camellia.redis.proxy.upstream.kv.kv;

import java.util.Base64;

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

    @Override
    public String toString() {
        Base64.Encoder encoder = Base64.getEncoder();
        String k = key == null ? null : encoder.encodeToString(key);
        String v = value == null ? null : encoder.encodeToString(value);
        return "key-value{k=" + k + ",v=" + v + "}";
    }
}
