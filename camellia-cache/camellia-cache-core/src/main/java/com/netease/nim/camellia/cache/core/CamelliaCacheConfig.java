package com.netease.nim.camellia.cache.core;

public class CamelliaCacheConfig<T extends INativeCache> implements ICamelliaCacheConfig {

    private final String name;
    private final boolean cacheNull;
    private final long expireMillis;
    private final T nativeCache;
    private final CamelliaCachePrefixGetter cachePrefixGetter;

    public CamelliaCacheConfig(CamelliaCachePrefixGetter cachePrefixGetter, String name, T nativeCache, boolean cacheNull, long expireMillis) {
        this.cachePrefixGetter = cachePrefixGetter;
        this.name = name;
        this.cacheNull = cacheNull;
        this.expireMillis = expireMillis;
        this.nativeCache = nativeCache;
    }

    public CamelliaCacheConfig(CamelliaCachePrefixGetter cachePrefixGetter, CamelliaCacheNameEnum camelliaCacheNameEnum, T nativeCache) {
        this.cachePrefixGetter = cachePrefixGetter;
        this.name = camelliaCacheNameEnum.getName();
        this.cacheNull = camelliaCacheNameEnum.isCacheNull();
        this.expireMillis = camelliaCacheNameEnum.getExpireMillis();
        this.nativeCache = nativeCache;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isCacheNull() {
        return cacheNull;
    }

    @Override
    public long getExpireMillis() {
        return expireMillis;
    }

    @Override
    public INativeCache getNativeCache() {
        return nativeCache;
    }

    @Override
    public CamelliaCachePrefixGetter getCachePrefixGetter() {
        return cachePrefixGetter;
    }
}
