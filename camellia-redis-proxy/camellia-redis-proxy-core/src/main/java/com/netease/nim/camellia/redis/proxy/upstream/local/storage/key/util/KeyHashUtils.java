package com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.util;

import java.util.Arrays;

/**
 * Created by caojiajun on 2025/1/2
 */
public class KeyHashUtils {

    public static int hash(byte[] key) {
        return Math.abs(Arrays.hashCode(key));
    }
}
