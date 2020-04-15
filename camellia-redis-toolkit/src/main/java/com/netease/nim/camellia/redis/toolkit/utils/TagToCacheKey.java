package com.netease.nim.camellia.redis.toolkit.utils;

/**
 * 将tag转成一个缓存Key的字符串
 * Created by caojiajun on 2020/4/9.
 */
public interface TagToCacheKey<T> {

    String toCacheKey(T tag);
}
