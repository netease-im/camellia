package com.netease.nim.camellia.redis.proxy.util;

/**
 *
 * Created by tasszz2k on 2022/11/11
 */
public class TenantUtils {

    private TenantUtils() {
        throw new IllegalStateException("Utility class");
    }

    private static final String KEY_SEPARATOR = "|";

    public static String buildKey(Long bid, String bgroup) {
        return bid + KEY_SEPARATOR + bgroup;
    }

}
