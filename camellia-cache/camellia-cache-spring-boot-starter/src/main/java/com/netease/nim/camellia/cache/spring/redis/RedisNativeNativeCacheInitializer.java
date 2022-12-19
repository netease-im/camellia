package com.netease.nim.camellia.cache.spring.redis;

import com.netease.nim.camellia.cache.spring.CamelliaCacheSerializer;
import com.netease.nim.camellia.cache.spring.RemoteNativeCache;
import com.netease.nim.camellia.cache.spring.RemoteNativeCacheInitializer;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;


public class RedisNativeNativeCacheInitializer implements RemoteNativeCacheInitializer {

    private CamelliaRedisTemplate redisTemplate;

    public void setRedisTemplate(CamelliaRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public RemoteNativeCache init(CamelliaCacheSerializer<Object> serializer) {
        if (redisTemplate == null) return null;
        return new RedisNativeCache(redisTemplate, serializer);
    }
}
