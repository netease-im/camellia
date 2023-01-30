package com.netease.nim.camellia.redis.toolkit.counter;

/**
 * 获取实际计数的回调方法
 * Created by caojiajun on 2020/4/9.
 */
public interface CounterGetter<T> {
    long get(T tag);
}
