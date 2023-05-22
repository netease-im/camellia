package com.netease.nim.camellia.tools.utils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * 锁map，主要作用是降低锁的粒度
 * Created by caojiajun on 2021/1/8
 */
public class LockMap {

    private final ConcurrentHashMap<String, Object> lockMap = new ConcurrentHashMap<>();

    /**
     * 通过 {@link java.util.Map#computeIfAbsent(Object, Function)}方法实现，如果存在就直接返回锁对象，如果不存在就调用 {@link java.util.function.Function} 接口 增加一个锁对象
     * @param key key
     * @return 锁对象
     */
    public Object getLockObj(String key) {
        return CamelliaMapUtils.computeIfAbsent(lockMap, key, k -> new Object());
    }
}
