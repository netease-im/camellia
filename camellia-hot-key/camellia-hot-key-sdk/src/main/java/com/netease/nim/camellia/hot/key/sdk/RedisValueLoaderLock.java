package com.netease.nim.camellia.hot.key.sdk;

import com.netease.nim.camellia.core.util.CacheUtil;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import com.netease.nim.camellia.redis.toolkit.lock.CamelliaRedisLock;

/**
 * Created by caojiajun on 2023/5/9
 */
public class RedisValueLoaderLock implements IValueLoaderLock {

    private final CamelliaRedisLock redisLock;

    private RedisValueLoaderLock(CamelliaRedisTemplate template, String prefix, String key, long expireMillis) {
        String lockKey = CacheUtil.buildCacheKey(prefix, key, "~lock");
        this.redisLock = CamelliaRedisLock.newLock(template, lockKey, expireMillis, expireMillis);
    }

    public static RedisValueLoaderLock newLock(CamelliaRedisTemplate template, String prefix, String key, long expireMillis) {
        return new RedisValueLoaderLock(template, prefix, key, expireMillis);
    }

    @Override
    public boolean tryLock() {
        return redisLock.tryLock();
    }

    @Override
    public boolean release() {
        return redisLock.release();
    }
}
