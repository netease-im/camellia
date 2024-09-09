package com.netease.nim.camellia.redis.proxy.upstream.kv.cache;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Created by caojiajun on 2024/9/9
 */
public class SlotCacheKey {
    private final byte[] key;
    private final int slot;
    private int hashCode;

    public SlotCacheKey(int slot, byte[] key) {
        this.slot = slot;
        this.key = key;
    }

    public byte[] getKey() {
        return key;
    }

    public int getSlot() {
        return slot;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;

        SlotCacheKey slotCacheKey = (SlotCacheKey) object;

        return Arrays.equals(key, slotCacheKey.key);
    }

    @Override
    public int hashCode() {
        if (hashCode == 0) {
            hashCode = Arrays.hashCode(key);
        }
        return hashCode;
    }

    @Override
    public String toString() {
        if (key == null) return null;
        return new String(key, StandardCharsets.UTF_8);
    }
}
