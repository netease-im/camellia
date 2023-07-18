package com.netease.nim.camellia.hot.key.sdk.util;

/**
 * Created by caojiajun on 2023/7/18
 */
public class HotKeySdkUtils {

    public static int update(int currentSize) {
        if (currentSize <= 0) return 2000;
        return Math.max(2000, Math.max(currentSize, currentSize + 1000));
    }
}
