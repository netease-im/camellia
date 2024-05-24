package com.netease.nim.camellia.redis.proxy.upstream.kv.buffer;

/**
 * Created by caojiajun on 2024/5/22
 */
public class WriteBufferValue<T> {
    private final T value;

    public WriteBufferValue(T value) {
        this.value = value;
    }

    public final T getValue() {
        return value;
    }
}
