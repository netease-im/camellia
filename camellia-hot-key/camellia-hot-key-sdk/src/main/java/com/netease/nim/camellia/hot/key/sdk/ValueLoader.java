package com.netease.nim.camellia.hot.key.sdk;

/**
 * Created by caojiajun on 2023/5/6
 */
public interface ValueLoader<T> {

    /**
     * load 一个value
     * @param key key
     * @return value
     */
    T load(String key);
}
