package com.netease.nim.camellia.redis.proxy.upstream.kv.cache;

/**
 * @param <T> element
 * Created by caojiajun on 2024/7/1
 */
public interface ValueWrapper<T> {
    T get();
}
