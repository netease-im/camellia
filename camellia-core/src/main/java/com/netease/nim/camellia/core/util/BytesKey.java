package com.netease.nim.camellia.core.util;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 *
 * Created by caojiajun on 2019/12/11.
 */
public class BytesKey {
    private final byte[] key;
    private int hashCode;

    public BytesKey(byte[] key) {
        this.key = key;
    }

    public byte[] getKey() {
        return key;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;

        BytesKey redisKey = (BytesKey) object;

        return Arrays.equals(key, redisKey.key);
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
