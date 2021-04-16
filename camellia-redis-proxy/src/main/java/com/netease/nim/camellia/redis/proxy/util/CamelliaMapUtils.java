package com.netease.nim.camellia.redis.proxy.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 *
 * Created by caojiajun on 2021/4/14
 */
public class CamelliaMapUtils {

    /**
     * jdk8 performance bug, see: https://bugs.openjdk.java.net/browse/JDK-8161372
     */
    public static <K, V> V computeIfAbsent(ConcurrentHashMap<K, V> map, K key, Function<? super K, ? extends V> mappingFunction) {
        V v = map.get(key);
        if (v != null) return v;
        return map.computeIfAbsent(key, mappingFunction);
    }
}
