package com.netease.nim.camellia.redis.base.utils;

/**
 *
 * Created by caojiajun on 2019/11/11.
 */
public class ProxyUtil {

    private static final String PREFIX = "camellia_";

    public static String buildClientName(long bid, String bgroup) {
        return PREFIX + bid + "_" + bgroup;
    }

    public static Long parseBid(String clientName) {
        try {
            if (clientName == null) return null;
            if (clientName.startsWith(PREFIX)) {
                return Long.parseLong(clientName.split("_")[1]);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public static String parseBgroup(String clientName) {
        try {
            if (clientName == null) return null;
            if (clientName.startsWith(PREFIX)) {
                return clientName.split("_")[2];
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
