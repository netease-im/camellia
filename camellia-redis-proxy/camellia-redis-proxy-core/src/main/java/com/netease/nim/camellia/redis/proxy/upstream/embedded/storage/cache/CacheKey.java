package com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.cache;

import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.EstimateSizeValue;

import java.util.Arrays;
import java.util.Objects;

/**
 * Created by caojiajun on 2025/1/8
 */
public record CacheKey(byte[] key) implements EstimateSizeValue {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CacheKey cacheKey = (CacheKey) o;
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
