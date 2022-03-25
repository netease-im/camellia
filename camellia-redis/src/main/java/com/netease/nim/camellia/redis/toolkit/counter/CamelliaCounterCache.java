package com.netease.nim.camellia.redis.toolkit.counter;

import com.netease.nim.camellia.core.util.DynamicValueGetter;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import com.netease.nim.camellia.redis.pipeline.ICamelliaRedisPipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Response;

/**
 * 一个计数器缓存，
 * 适用于count计算比较耗时，且对计数要求不是非常准确，但是比较关心是否超过阈值的情况（希望尽可能不要超限，但真的偶尔超限了也能接受）
 * Created by caojiajun on 2020/4/9.
 */
public class CamelliaCounterCache<T> {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaCounterCache.class);

    private CounterGetter<T> counterGetter;//获取精确值的回调方法

    //是否开启
    private DynamicValueGetter<Boolean> cacheEnable = () -> true;

    private CamelliaRedisTemplate template;//redis客户端
    private TagToCacheKey<T> tagToCacheKey;//tag转成缓存key
    private DynamicValueGetter<Long> threshold;//计数器阈值
    private DynamicValueGetter<Integer> expireSeconds;//计数器缓存时长
    private DynamicValueGetter<Integer> adjustCacheIntervalSeconds;//缓存多久更新一次
    private DynamicValueGetter<Integer> exceedThresholdAdjustCacheIntervalSeconds;//阈值超限的情况下，缓存多久更新一次

    private CamelliaCounterCache() {
    }

    public static class Builder<T> {
        private final CamelliaCounterCache<T> counterCache = new CamelliaCounterCache<>();
        public Builder() {
        }

        public Builder<T> counterGetter(CounterGetter<T> counterGetter) {
            counterCache.counterGetter = counterGetter;
            return this;
        }

        public Builder<T> redisTemplate(CamelliaRedisTemplate template) {
            counterCache.template = template;
            return this;
        }

        public Builder<T> tagToCacheKey(TagToCacheKey<T> tagToCacheKey) {
            counterCache.tagToCacheKey = tagToCacheKey;
            return this;
        }

        public Builder<T> threshold(DynamicValueGetter<Long> threshold) {
            counterCache.threshold = threshold;
            return this;
        }

        public Builder<T> expireSeconds(DynamicValueGetter<Integer> expireSeconds) {
            counterCache.expireSeconds = expireSeconds;
            return this;
        }

        public Builder<T> adjustCacheIntervalSeconds(DynamicValueGetter<Integer> adjustCacheIntervalSeconds) {
            counterCache.adjustCacheIntervalSeconds = adjustCacheIntervalSeconds;
            return this;
        }

        public Builder<T> cacheEnable(DynamicValueGetter<Boolean> cacheEnable) {
            counterCache.cacheEnable = cacheEnable;
            return this;
        }

        public Builder<T> exceedThresholdAdjustCacheIntervalSeconds(DynamicValueGetter<Integer> exceedThresholdAdjustCacheIntervalSeconds) {
            counterCache.exceedThresholdAdjustCacheIntervalSeconds = exceedThresholdAdjustCacheIntervalSeconds;
            return this;
        }

        public CamelliaCounterCache<T> build() {
            if (counterCache.counterGetter == null) {
                throw new IllegalArgumentException("counterGetter required");
            }
            if (counterCache.cacheEnable == null) {
                throw new IllegalArgumentException("cacheEnable required");
            }
            if (counterCache.template == null) {
                throw new IllegalArgumentException("redisTemplate required");
            }
            if (counterCache.tagToCacheKey == null) {
                throw new IllegalArgumentException("tagToCacheKey required");
            }
            if (counterCache.threshold == null) {
                throw new IllegalArgumentException("threshold required");
            }
            if (counterCache.expireSeconds == null) {
                throw new IllegalArgumentException("expireSeconds required");
            }
            if (counterCache.adjustCacheIntervalSeconds == null) {
                throw new IllegalArgumentException("adjustCacheIntervalSeconds required");
            }
            if (counterCache.exceedThresholdAdjustCacheIntervalSeconds == null) {
                throw new IllegalArgumentException("exceedThresholdAdjustCacheIntervalSeconds required");
            }
            return counterCache;
        }
    }

    /**
     * 获取计数
     * @param tag 业务tag
     * @return 计数
     */
    public long getCount(T tag) {
        if (!cacheEnable.get()) return counterGetter.get(tag);
        String cacheKey = tagToCacheKey.toCacheKey(tag);
        String lastSyncKey = cacheKey + "~lastSync";
        try {
            Long countCache = null;
            Long lastSyncTime = null;
            try (ICamelliaRedisPipeline pipeline = template.pipelined()) {
                Response<String> response1 = pipeline.get(cacheKey);
                Response<String> response2 = pipeline.get(lastSyncKey);
                pipeline.sync();
                String countCacheValue = response1.get();
                String lastSyncTimeCacheValue = response2.get();
                if (countCacheValue != null && lastSyncTimeCacheValue != null) {
                    try {
                        countCache = Long.parseLong(countCacheValue);
                        lastSyncTime = Long.parseLong(lastSyncTimeCacheValue);
                    } catch (NumberFormatException e) {
                        template.del(cacheKey, lastSyncKey);
                    }
                }
            }
            long now = System.currentTimeMillis();
            if (countCache != null && lastSyncTime != null) {
                if (countCache < threshold.get()) {
                    if (now - lastSyncTime > adjustCacheIntervalSeconds.get() * 1000L) {
                        return rebuildCache(cacheKey, lastSyncKey, tag);
                    } else {
                        return countCache;
                    }
                } else {
                    if (now - lastSyncTime > exceedThresholdAdjustCacheIntervalSeconds.get() * 1000L) {
                        return rebuildCache(cacheKey, lastSyncKey, tag);
                    } else {
                        return countCache;
                    }
                }
            } else {
                return rebuildCache(cacheKey, lastSyncKey, tag);
            }
        } catch (Exception e) {
            logger.error("getCount error, tag = {}", tag, e);
            long dbCount = counterGetter.get(tag);
            try {
                template.del(cacheKey, lastSyncKey);
            } catch (Exception ex) {
                logger.error("del error, tag = {}", tag, ex);
            }
            return dbCount;
        }
    }

    /**
     * 判断计数是否超限了
     */
    public boolean checkCountExceed(T tag) {
        long count = getCount(tag);
        return count > threshold.get();
    }

    //重建缓存
    private long rebuildCache(String cacheKey, String lastSyncKey, T tag) {
        long dbCount = counterGetter.get(tag);
        try (ICamelliaRedisPipeline pipeline = template.pipelined()) {
            pipeline.setex(cacheKey, expireSeconds.get(), String.valueOf(dbCount));
            pipeline.setex(lastSyncKey, expireSeconds.get(), String.valueOf(System.currentTimeMillis()));
            pipeline.sync();
        }
        return dbCount;
    }

    /**
     * 计数+count
     */
    public long incrBy(T tag, long count) {
        if (!cacheEnable.get()) return -1;
        try {
            String cacheKey = tagToCacheKey.toCacheKey(tag);
            Boolean exists = template.exists(cacheKey);
            if (exists != null && exists) {
                return template.incrBy(cacheKey, count);
            }
            return -1;
        } catch (Exception e) {
            logger.error("incrBy error, tag = {}", tag, e);
            return -1;
        }
    }

    /**
     * 计数+1
     */
    public long incr(T tag) {
        if (!cacheEnable.get()) return -1;
        try {
            String cacheKey = tagToCacheKey.toCacheKey(tag);
            Boolean exists = template.exists(cacheKey);
            if (exists != null && exists) {
                return template.incr(cacheKey);
            }
            return -1;
        } catch (Exception e) {
            logger.error("incr error, tag = {}", tag, e);
            return -1;
        }
    }

    /**
     * 计数-1
     */
    public long decr(T tag) {
        if (!cacheEnable.get()) return -1;
        try {
            String cacheKey = tagToCacheKey.toCacheKey(tag);
            Boolean exists = template.exists(cacheKey);
            if (exists != null && exists) {
                return template.decr(cacheKey);
            }
            return -1;
        } catch (Exception e) {
            logger.error("decr error, tag = {}", tag, e);
            return -1;
        }
    }

    /**
     * 计数-count
     */
    public long decrBy(T tag, long count) {
        if (!cacheEnable.get()) return -1;
        try {
            String cacheKey = tagToCacheKey.toCacheKey(tag);
            Boolean exists = template.exists(cacheKey);
            if (exists != null && exists) {
                return template.decrBy(cacheKey, count);
            }
            return -1;
        } catch (Exception e) {
            logger.error("decrBy error, tag = {}", tag, e);
            return -1;
        }
    }
}
