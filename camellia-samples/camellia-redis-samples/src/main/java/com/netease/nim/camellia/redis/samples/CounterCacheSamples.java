package com.netease.nim.camellia.redis.samples;

import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import com.netease.nim.camellia.redis.toolkit.counter.CamelliaCounterCache;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * Created by caojiajun on 2020/4/14.
 */
public class CounterCacheSamples {

    private static final ConcurrentHashMap<Long, AtomicLong> db = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        CamelliaRedisTemplate template = new CamelliaRedisTemplate("redis://abc@127.0.0.1:6379");

        CamelliaCounterCache<Long> counterCache = new CamelliaCounterCache.Builder<Long>()
                .redisTemplate(template)
                .adjustCacheIntervalSeconds(() -> 30)//缓存多久更新一次
                .exceedThresholdAdjustCacheIntervalSeconds(() -> 5)//阈值超限的情况下，缓存多久更新一次
                .tagToCacheKey(tag -> "counter_" + tag)//tag转成缓存key
                .threshold(() -> 100L)//计数器阈值
                .expireSeconds(() -> 3600)//计数器缓存时长
                .counterGetter(tag -> {//获取精确值的回调方法
                    AtomicLong count = db.computeIfAbsent(tag, k -> new AtomicLong(0));
                    System.out.println("hit to db, tag = " + tag);
                    return count.get();
                }).build();

        Long tag = 100L;
        System.out.println(counterCache.getCount(tag));

        incrDb(tag);
        counterCache.incr(tag);

        System.out.println(counterCache.getCount(tag));
    }

    private static void incrDb(Long tag) {
        AtomicLong count = db.computeIfAbsent(tag, k -> new AtomicLong(0));
        count.incrementAndGet();
    }
}
