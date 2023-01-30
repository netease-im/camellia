package com.netease.nim.camellia.redis.base.utils;

import java.io.Closeable;

/**
 *
 * Created by caojiajun on 2019/8/8.
 */
public class CloseUtil {

    public static void closeQuietly(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (Exception ignore) {
        }
    }
}
