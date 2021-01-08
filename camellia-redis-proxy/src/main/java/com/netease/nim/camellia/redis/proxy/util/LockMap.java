package com.netease.nim.camellia.redis.proxy.util;

import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * Created by caojiajun on 2021/1/8
 */
public class LockMap {

    private final ConcurrentHashMap<String, Object> lockMap = new ConcurrentHashMap<>();

    public Object getLockObj(String key) {
        return lockMap.computeIfAbsent(key, k -> new Object());
    }
}
