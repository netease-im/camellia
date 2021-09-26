package com.netease.nim.camellia.core.util;

/**
 *
 * Created by caojiajun on 2018/6/27.
 */
public class CacheUtil {

    public static String buildCacheKey(String tag, Object...obj) {
        StringBuilder key = new StringBuilder(tag);
        if (obj != null) {
            key.append("|");
            for (int i=0; i<obj.length; i++) {
                if (i == obj.length - 1) {
                    key.append(obj[i]);
                } else {
                    key.append(obj[i]).append("|");
                }
            }
        }
        return key.toString();
    }
}
