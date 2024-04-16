package com.netease.nim.camellia.redis.proxy.kv.core.domain;

import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;

/**
 * Created by caojiajun on 2024/4/8
 */
public class CacheConfig {

    private final String namespace;
    private final boolean cacheEnable;

    public CacheConfig(String namespace, boolean cacheEnable) {
        this.namespace = namespace;
        this.cacheEnable = cacheEnable;
    }

    public boolean isCacheEnable() {
        return cacheEnable;
    }

    public long keyMetaTimeoutMillis() {
        long keyMetaTimeoutMillis = ProxyDynamicConf.getLong(namespace + ".kv.key.meta.timeout.millis", -1L);
        if (keyMetaTimeoutMillis > 0) {
            return keyMetaTimeoutMillis;
        }
        return ProxyDynamicConf.getLong("kv.key.meta.timeout.millis", 2000L);
    }

    public long cacheTimeoutMillis() {
        long cacheTimeoutMillis = ProxyDynamicConf.getLong(namespace + ".kv.cache.timeout.millis", -1L);
        if (cacheTimeoutMillis > 0) {
            return cacheTimeoutMillis;
        }
        return ProxyDynamicConf.getLong("kv.cache.timeout.millis", 2000L);
    }

    public long hgetCacheMillis() {
        long hgetCacheMillis = ProxyDynamicConf.getLong(namespace + ".kv.cache.hget.cache.millis", -1L);
        if (hgetCacheMillis > 0) {
            return hgetCacheMillis;
        }
        return ProxyDynamicConf.getLong("kv.cache.hget.cache.millis", 5*60*1000L);
    }

    public long hgetallCacheMillis() {
        long hgetallCacheMillis = ProxyDynamicConf.getLong(namespace + ".kv.cache.hgetall.cache.millis", -1L);
        if (hgetallCacheMillis > 0) {
            return hgetallCacheMillis;
        }
        return ProxyDynamicConf.getLong("kv.cache.hgetall.cache.millis", 5*60*1000L);
    }
}
