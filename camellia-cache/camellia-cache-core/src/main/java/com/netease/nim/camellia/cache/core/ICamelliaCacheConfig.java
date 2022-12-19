package com.netease.nim.camellia.cache.core;

public interface ICamelliaCacheConfig {

    String getName();

    boolean isCacheNull();

    long getExpireMillis();

    INativeCache getNativeCache();

    CamelliaCachePrefixGetter getCachePrefixGetter();
}
