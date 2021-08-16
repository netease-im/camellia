package com.netease.nim.camellia.redis.proxy.hbase.util;

/**
 *
 * Created by caojiajun on 2021/7/26
 */
public class HBaseValue {
    private byte[] value;
    private boolean expire;
    private Long ttlMillis;

    public HBaseValue(byte[] value, Long ttlMillis, boolean expire) {
        this.value = value;
        this.expire = expire;
        this.ttlMillis = ttlMillis;
    }

    public byte[] getValue() {
        return value;
    }

    public void setValue(byte[] value) {
        this.value = value;
    }

    public boolean isExpire() {
        return expire;
    }

    public void setExpire(boolean expire) {
        this.expire = expire;
    }

    public Long getTtlMillis() {
        return ttlMillis;
    }

    public void setTtlMillis(Long ttlMillis) {
        this.ttlMillis = ttlMillis;
    }
}
