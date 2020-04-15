package com.netease.nim.camellia.redis.toolkit.id;

import com.netease.nim.camellia.core.util.CamelliaThreadFactory;
import com.netease.nim.camellia.core.util.SysUtils;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import com.netease.nim.camellia.redis.pipeline.ICamelliaRedisPipeline;
import com.netease.nim.camellia.redis.toolkit.localcache.LocalCache;
import com.netease.nim.camellia.redis.toolkit.lock.CamelliaRedisLock;
import com.netease.nim.camellia.redis.toolkit.utils.DynamicValueGetter;
import com.netease.nim.camellia.redis.toolkit.utils.TagToCacheKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 构造严格递增序列的ID生成器
 * 使用redis的队列实现
 * 可以实现动态调整的步长
 * Created by caojiajun on 2020/4/9.
 */
public class CamelliaIDGenerator<T> {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaIDGenerator.class);

    private CamelliaRedisTemplate template;

    private IDLoader<T> idLoader;

    private TagToCacheKey<T> tagToCacheKey;//tag转成缓存key

    //默认步长（每次从db获取一段id的数量的默认值，也是最小步长）
    private DynamicValueGetter<Integer> defaultStep = new DynamicValueGetter<Integer>() {
        @Override
        public Integer get() {
            return 5;
        }
    };

    //最大步长（从db获取一段id的数量的最大值）
    private DynamicValueGetter<Integer> maxStep = new DynamicValueGetter<Integer>() {
        @Override
        public Integer get() {
            return 1000;
        }
    };

    //id在redis里缓存的时间
    private DynamicValueGetter<Integer> expireSeconds = new DynamicValueGetter<Integer>() {
        @Override
        public Integer get() {
            return 7*24*3600;
        }
    };

    //每次从db获取一段id时获取的并发锁的有效期
    private DynamicValueGetter<Integer> lockExpireSeconds = new DynamicValueGetter<Integer>() {
        @Override
        public Integer get() {
            return 10;
        }
    };

    //如果从db获取的一段id在这个时间段内就触发了loadFromDb，则调整步长（小于则增大步长，大于则减少步长）
    private DynamicValueGetter<Integer> cacheHoldSeconds = new DynamicValueGetter<Integer>() {
        @Override
        public Integer get() {
            return 600;
        }
    };

    //没有获取到ID的时候最大重试次数
    private DynamicValueGetter<Integer> maxRetry = new DynamicValueGetter<Integer>() {
        @Override
        public Integer get() {
            return 3;
        }
    };

    //异步检查线程池
    private ExecutorService asyncExecutor = new ThreadPoolExecutor(SysUtils.getCpuNum()*4, SysUtils.getCpuNum()*4, 0L,
            TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(1024), new CamelliaThreadFactory(CamelliaIDGenerator.class),
            new ThreadPoolExecutor.AbortPolicy());

    //本地缓存
    private LocalCache localCache = new LocalCache();

    private CamelliaIDGenerator() {
    }

    public static class Builder<T> {
        private CamelliaIDGenerator<T> generator = new CamelliaIDGenerator<>();
        public Builder() {
        }

        public Builder<T> redisTemplate(CamelliaRedisTemplate template) {
            generator.template = template;
            return this;
        }

        public Builder<T> tagToCacheKey(TagToCacheKey<T> tagToCacheKey) {
            generator.tagToCacheKey = tagToCacheKey;
            return this;
        }

        public Builder<T> defaultStep(DynamicValueGetter<Integer> defaultStep) {
            generator.defaultStep = defaultStep;
            return this;
        }

        public Builder<T> maxStep(DynamicValueGetter<Integer> maxStep) {
            generator.maxStep = maxStep;
            return this;
        }

        public Builder<T> expireSeconds(DynamicValueGetter<Integer> expireSeconds) {
            generator.expireSeconds = expireSeconds;
            return this;
        }

        public Builder<T> lockExpireSeconds(DynamicValueGetter<Integer> lockExpireSeconds) {
            generator.lockExpireSeconds = lockExpireSeconds;
            return this;
        }

        public Builder<T> cacheHoldSeconds(DynamicValueGetter<Integer> cacheHoldSeconds) {
            generator.cacheHoldSeconds = cacheHoldSeconds;
            return this;
        }

        public Builder<T> asyncExecutor(ExecutorService asyncExecutor) {
            generator.asyncExecutor = asyncExecutor;
            return this;
        }

        public Builder<T> localCacheSize(int size) {
            generator.localCache = new LocalCache(size);
            return this;
        }

        public Builder<T> idLoader(IDLoader<T> idLoader) {
            generator.idLoader = idLoader;
            return this;
        }

        public CamelliaIDGenerator<T> build() {
            if (generator.template == null) {
                throw new CamelliaIDGenerateException("redisTemplate required");
            }
            if (generator.idLoader == null) {
                throw new CamelliaIDGenerateException("idLoader required");
            }
            if (generator.tagToCacheKey == null) {
                throw new CamelliaIDGenerateException("cachePrefix required");
            }
            return generator;
        }
    }

    /**
     * 生成一个id
     * @param tag 业务tag
     * @return id
     */
    public long generate(final T tag) {
        try {
            String cacheKey = tagToCacheKey.toCacheKey(tag);
            //从redis里取一下，如果取到了，则返回
            String seq = template.rpop(cacheKey);
            if (seq != null) {
                try {
                    return Long.parseLong(seq);
                } finally {
                    try {
                        //此时提交一个异步任务，检查是否需要更新redis队列
                        asyncExecutor.submit(new Runnable() {
                            @Override
                            public void run() {
                                checkAndLoad(tag);
                            }
                        });
                    } catch (Exception e) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("try checkAndLoad fail, tag = {}", tag);
                        }
                    }
                }
            }
            //redis里没有取到，则更新redis队列
            tryLoadIds(tag);
            //更新完redis队列，从redis队列里取元素
            int retry = maxRetry.get();
            while (retry-- > 0) {
                for (int i = 0; i < 10; i++) {
                    String rpop = template.rpop(cacheKey);
                    if (rpop == null) {
                        //取不到，自旋一下
                        sleep(5);
                        continue;
                    }
                    //取到了直接返回
                    return Long.parseLong(rpop);
                }
                //连续取不到，则判断一下是否redis队列被消耗完了
                tryLoadIds(tag);
            }
        } catch (Exception e) {
            throw new CamelliaIDGenerateException(e);
        }
        //超过最大重试次数，则上抛异常
        throw new CamelliaIDGenerateException("exceed maxRetry=" + maxRetry.get());
    }

    private static final String TAG = "T";
    private void checkAndLoad(final T tag) {
        String cacheKey = tagToCacheKey.toCacheKey(tag);

        Long size = template.llen(cacheKey);
        if (size == null || size <= 0) {//redis里空了，则需要触发一下load
            if (logger.isDebugEnabled()) {
                logger.debug("cacheKey = {}, cache.size=0, try loadIds", cacheKey);
            }
            tryLoadIds(tag);
        } else {
            String currentStepStr = template.get(currentStepKey(cacheKey));
            int currentStep = currentStepStr != null ? Integer.parseInt(currentStepStr) : defaultStep.get();
            if (size < currentStep / 2) {//如果redis里不足一半了，则需要触发一下load
                if (logger.isDebugEnabled()) {
                    logger.debug("cacheKey = {}, cache.size/currentStep={}/{}, try loadIds", cacheKey, size, currentStep);
                }
                tryLoadIds(tag);
            } else {
                Long lastExpireTime = localCache.get(TAG, cacheKey, Long.class);
                if (lastExpireTime != null && (System.currentTimeMillis() - lastExpireTime) / 1000 < expireSeconds.get() / 10) {
                    return;
                }
                //否则，delay一下相关redis key
                try (ICamelliaRedisPipeline pipelined = template.pipelined()) {
                    pipelined.expire(cacheKey, expireSeconds.get());
                    pipelined.expire(cacheHoldKey(cacheKey), expireSeconds.get());
                    pipelined.expire(currentStepKey(cacheKey), expireSeconds.get());
                    pipelined.sync();
                    localCache.put(TAG, cacheKey, System.currentTimeMillis(), -1);
                }
            }
        }
    }

    private void tryLoadIds(T tag) {
        String cacheKey = tagToCacheKey.toCacheKey(tag);
        String lockKey = lockKey(cacheKey);
        String cacheHoldKey = cacheHoldKey(cacheKey);
        String currentStepKey = currentStepKey(cacheKey);

        CamelliaRedisLock lock = CamelliaRedisLock.newLock(template, lockKey, lockExpireSeconds.get(), lockExpireSeconds.get());
        boolean tryLock = lock.tryLock();
        if (tryLock) {
            try {
                if (logger.isDebugEnabled()) {
                    logger.debug("acquire lock success, cacheKey = {}, lockKey = {}, will loadFromDb", cacheKey, lockKey);
                }

                String lastSyncTimeStr;
                String currentStepStr;
                try (ICamelliaRedisPipeline pipelined = template.pipelined()) {
                    Response<String> response1 = pipelined.get(cacheHoldKey);
                    Response<String> response2 = pipelined.get(currentStepKey);
                    pipelined.sync();
                    lastSyncTimeStr = response1.get();
                    currentStepStr = response2.get();
                }

                int newStep = defaultStep.get();
                if (lastSyncTimeStr != null && currentStepStr != null) {
                    long lastSyncTime = Long.parseLong(lastSyncTimeStr);
                    int currentStep = Integer.parseInt(currentStepStr);
                    long now = System.currentTimeMillis();
                    //动态调整步长
                    if (now - lastSyncTime < cacheHoldSeconds.get()) {
                        newStep = Math.min(currentStep * 2, maxStep.get());
                        if (logger.isDebugEnabled()) {
                            logger.debug("step try expand, cacheKey = {}, old/new={}/{}", cacheKey, currentStep, newStep);
                        }
                    } else if (now - lastSyncTime > cacheHoldSeconds.get() * 2) {
                        newStep = Math.max(currentStep / 2, defaultStep.get());
                        if (logger.isDebugEnabled()) {
                            logger.debug("step try shrink, cacheKey = {}, old/new={}/{}", cacheKey, currentStep, newStep);
                        }
                    }
                }
                IDRange range = idLoader.load(tag, newStep);
                List<String> ids = new ArrayList<>();
                for (long i = range.getStart(); i<= range.getEnd(); i++) {
                    ids.add(String.valueOf(i));
                }
                try (ICamelliaRedisPipeline pipelined = template.pipelined()) {
                    pipelined.lpush(cacheKey, ids.toArray(new String[0]));
                    pipelined.expire(cacheKey, expireSeconds.get());
                    pipelined.setex(cacheHoldKey, expireSeconds.get(), String.valueOf(System.currentTimeMillis()));
                    pipelined.setex(currentStepKey, expireSeconds.get(), String.valueOf(newStep));
                    pipelined.sync();
                    if (logger.isDebugEnabled()) {
                        logger.debug("loadId success, cacheKey = {}, start = {}, end = {}", cacheKey, range.getStart(), range.getEnd());
                    }
                }
            } finally {
                lock.release();
            }
        }
    }

    private String lockKey(String cacheKey) {
        return cacheKey + "~lock";
    }

    private String cacheHoldKey(String cacheKey) {
        return cacheKey + "~cacheHold";
    }

    private String currentStepKey(String cacheKey) {
        return cacheKey + "~currentStep";
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            logger.error("sleep error", e);
        }
    }
}
