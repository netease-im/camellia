package com.netease.nim.camellia.cache.spring;


public interface RemoteNativeCacheInitializer {

    RemoteNativeCache init(CamelliaCacheSerializer<Object> serializer);
}
