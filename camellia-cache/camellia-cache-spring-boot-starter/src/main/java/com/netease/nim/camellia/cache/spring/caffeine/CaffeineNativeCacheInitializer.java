package com.netease.nim.camellia.cache.spring.caffeine;


import com.netease.nim.camellia.cache.spring.CamelliaCacheSerializer;
import com.netease.nim.camellia.cache.spring.LocalNativeCache;
import com.netease.nim.camellia.cache.spring.LocalNativeCacheInitializer;

public class CaffeineNativeCacheInitializer implements LocalNativeCacheInitializer {

    @Override
    public LocalNativeCache init(CamelliaCacheSerializer<Object> serializer, int initialCapacity, int capacity, boolean safe) {
        return new CaffeineNativeCache(initialCapacity, capacity, safe, serializer);
    }
}
