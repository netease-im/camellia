package com.netease.nim.camellia.redis.proxy.util;

/**
 * Created by caojiajun on 2022/9/16
 */
public class BeanInitUtils {

    public static Class<?> parseClass(String className) {
        try {
            Class<?> clazz;
            try {
                clazz = Class.forName(className);
            } catch (ClassNotFoundException e) {
                clazz = Thread.currentThread().getContextClassLoader().loadClass(className);
            }
            return clazz;
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

}
