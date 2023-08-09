package com.netease.nim.camellia.redis.proxy.tls.upstream;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.redis.base.resource.RedisType;
import com.netease.nim.camellia.redis.proxy.util.ExecutorUtils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by caojiajun on 2023/8/9
 */
public class RedisResourceTlsEnableCache {
    private static final ConcurrentHashMap<String, Boolean> cache = new ConcurrentHashMap<>();

    static {
        ExecutorUtils.scheduleAtFixedRate(cache::clear, 1, 1, TimeUnit.HOURS);
    }

    public static boolean tlsEnable(Resource resource) {
        Boolean tlsEnable = cache.get(resource.getUrl());
        if (tlsEnable != null && !tlsEnable) {
            return false;
        }
        RedisType redisType = RedisType.parseRedisType(resource);
        if (redisType == null) {
            cache.put(resource.getUrl(), false);
            return false;
        }
        cache.put(resource.getUrl(), redisType.isTlsEnable());
        return redisType.isTlsEnable();
    }
}
