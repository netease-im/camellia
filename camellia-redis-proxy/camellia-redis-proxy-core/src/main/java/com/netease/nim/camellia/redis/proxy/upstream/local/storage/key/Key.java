package com.netease.nim.camellia.redis.proxy.upstream.local.storage.key;

import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.EstimateSizeValue;

import java.util.Arrays;
import java.util.Objects;

/**
 * Created by caojiajun on 2025/1/8
 */
public record Key(byte[] key) implements EstimateSizeValue {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Key cacheKey = (Key) o;
        return Objects.deepEquals(key, cacheKey.key);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(key);
    }

    @Override
    public long estimateSize() {
        return key.length;
    }
}
