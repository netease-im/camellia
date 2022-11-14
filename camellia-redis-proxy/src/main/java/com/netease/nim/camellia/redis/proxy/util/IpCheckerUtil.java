package com.netease.nim.camellia.redis.proxy.util;

/**
 * @author tasszz2k
 * @since 11/11/2022
 */
public class IpCheckerUtil {

    private static final String KEY_SEPARATOR = "|";

    public static String buildIpCheckerKey(Long bid, String bgroup) {
        return bid + KEY_SEPARATOR + bgroup;
    }

}
