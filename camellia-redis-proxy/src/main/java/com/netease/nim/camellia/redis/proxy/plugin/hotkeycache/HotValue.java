package com.netease.nim.camellia.redis.proxy.plugin.hotkeycache;

/**
 *
 * Created by caojiajun on 2020/11/4
 */
public class HotValue {

    private final byte[] value;

    public HotValue(byte[] value) {
        this.value = value;
    }

    public byte[] getValue() {
        return value;
    }
}
