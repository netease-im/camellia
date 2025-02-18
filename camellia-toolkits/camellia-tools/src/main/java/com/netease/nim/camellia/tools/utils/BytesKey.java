package com.netease.nim.camellia.tools.utils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 *
 * Created by caojiajun on 2019/12/11.
 */
public class BytesKey {
    private final byte[] key;

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
        return Arrays.hashCode(key);
    }

    @Override
    public String toString() {
        if (key == null) return null;
        return new String(key, StandardCharsets.UTF_8);
    }
}
