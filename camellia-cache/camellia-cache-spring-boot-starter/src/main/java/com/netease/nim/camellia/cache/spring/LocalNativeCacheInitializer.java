package com.netease.nim.camellia.cache.spring;


public interface LocalNativeCacheInitializer {

    LocalNativeCache init(CamelliaCacheSerializer<Object> serializer, int initialCapacity, int capacity, boolean safe);
}
