package com.netease.nim.camellia.redis.proxy.util;

import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;

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

    public static String getClassName(String configPrefix, String defaultClassName) {
        String className = ProxyDynamicConf.getString(configPrefix + ".className", null);
        if (className == null) {
            className = ProxyDynamicConf.getString(configPrefix + ".class.name", null);
        }
        if (className == null) {
            return defaultClassName;
        }
        return className;
    }

}
