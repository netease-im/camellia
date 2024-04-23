package com.netease.nim.camellia.redis.proxy.upstream.kv.conf;

import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;

/**
 * Created by caojiajun on 2024/4/17
 */
public class RedisKvConf {

    public static int getInt(String namespace, String key, int defaultValue) {
        return ProxyDynamicConf.getInt(namespace + "." + key, ProxyDynamicConf.getInt(key, defaultValue));
    }

    public static long getLong(String namespace, String key, long defaultValue) {
        return ProxyDynamicConf.getLong(namespace + "." + key, ProxyDynamicConf.getLong(key, defaultValue));
    }

    public static String getString(String namespace, String key, String defaultValue) {
        return ProxyDynamicConf.getString(namespace + "." + key, ProxyDynamicConf.getString(key, defaultValue));
    }

    public static boolean getBoolean(String namespace, String key, boolean defaultValue) {
        return ProxyDynamicConf.getBoolean(namespace + "." + key, ProxyDynamicConf.getBoolean(key, defaultValue));
    }
}
