package com.netease.nim.camellia.tools.utils;

import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * Created by caojiajun on 2021/1/8
 */
public class LockMap {

    private final ConcurrentHashMap<String, Object> lockMap = new ConcurrentHashMap<>();

    public Object getLockObj(String key) {
        return CamelliaMapUtils.computeIfAbsent(lockMap, key, k -> new Object());
    }
}
