package com.netease.nim.camellia.redis.proxy.upstream.kv.cache;

/**
 * Created by caojiajun on 2024/7/1
 */
public interface ValueWrapper<T> {
    T get();
}
