package com.netease.nim.camellia.tools;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 一个LRU的缓存，并且会在后台线程定时更新那些刚刚访问过的热key
 * 特别的，当缓存穿透时，如果更新缓存操作出现异常，会返回上一次获取到的缓存旧值
 *
 * 对比Caffeine的refreshAfterWrite策略：
 * 当一个key很久没有访问过了，此时调用get方法，会触发一个缓存更新操作，但是当前的请求可能返回一个过期的数据，相反CamelliaLoadingCache会确保返回最新的
 *
 * 对比Caffeine的expireAfterWrite策略：
 * 当一个key很久没有访问过了，此时调用get方法，会触发一个缓存更新操作，但是如果缓存更新失败了会返回一个异常，相反CamelliaLoadingCache会返回一个旧值
 *
 * CamelliaLoadingCache适合于一些特定的使用场景，请按需使用
 *
 * Created by caojiajun on 2021/8/26
 */
public class CamelliaLoadingCache<K, V> {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaLoadingCache.class);

    private static final AtomicLong idGenerator = new AtomicLong(0);

    private static volatile long now = System.currentTimeMillis();
    static {
        Thread timeCache = new Thread(() -> {
            while (true) {
                now = System.currentTimeMillis();
                try {
                    TimeUnit.MILLISECONDS.sleep(50);
                } catch (InterruptedException ignore) {
                }
            }
        }, "camellia-loading-cache-time");
        timeCache.setDaemon(true);
        timeCache.start();
    }

    private final ConcurrentLinkedHashMap<K, ValueInfo> cacheMap;
    private final ConcurrentLinkedHashMap<K, AtomicBoolean> lockMap;

    private final CacheLoader<K, V> cacheLoader;
    private final long expireMillis;
    private final boolean cacheNull;
    private final int concurrentMaxRetry;
    private final long concurrentSleepMs;

    private CamelliaLoadingCache(CacheLoader<K, V> cacheLoader, int initialCapacity, int maxCapacity,
                                 long expireMillis, boolean cacheNull, int concurrentMaxRetry, long concurrentSleepMs) {
        this.cacheLoader = cacheLoader;
        this.expireMillis = expireMillis;
        this.cacheNull = cacheNull;
        this.concurrentMaxRetry = concurrentMaxRetry;
        this.concurrentSleepMs = concurrentSleepMs;
        cacheMap = new ConcurrentLinkedHashMap.Builder<K, ValueInfo>()
                .initialCapacity(initialCapacity).maximumWeightedCapacity(maxCapacity).build();
        lockMap = new ConcurrentLinkedHashMap.Builder<K, AtomicBoolean>()
                .initialCapacity(initialCapacity).maximumWeightedCapacity(maxCapacity).build();
        //起一个后台线程来定时更新最近刚刚访问过的key的value
        Thread thread = new Thread(() -> {
            while (true) {
                try {
                    TimeUnit.MILLISECONDS.sleep(expireMillis / 5);
                    reload();
                } catch (Exception e) {
                    logger.error("reload error", e);
                }
            }
        }, "camellia-loading-cache-" + idGenerator.incrementAndGet());
        thread.setDaemon(true);
        thread.start();
    }

    public static class Builder<K, V> {
        private int initialCapacity = 1000;
        private int maxCapacity = 10000;
        private long expireMillis = 10000;
        private boolean cacheNull = true;
        private int concurrentMaxRetry = 5;
        private long concurrentSleepMs = 1;

        public Builder() {
        }

        //缓存的初始容量
        public Builder<K, V> initialCapacity(int initialCapacity) {
            this.initialCapacity = initialCapacity;
            return this;
        }

        //缓存的最大容量
        public Builder<K, V> maxCapacity(int maxCapacity) {
            this.maxCapacity = maxCapacity;
            return this;
        }

        //缓存过期时间，单位ms
        public Builder<K, V> expireMillis(long expireMillis) {
            this.expireMillis = expireMillis;
            if (expireMillis < 1000) {
                throw new IllegalArgumentException("expireMillis should greater than 1000ms");
            }
            return this;
        }

        //是否缓存null
        public Builder<K, V> cacheNull(boolean cacheNull) {
            this.cacheNull = cacheNull;
            return this;
        }

        //并发缓存穿透时会控制穿透的并发，如果出现了竞争，则只有一个线程会调用CacheLoader.load()
        //其他线程会等待并重试，重试时会检查缓存是否已经更新好了，如果已经更新过了，则会返回更新后的缓存，本配置表示重试的最大次数
        //当超过重试次数时会直接穿透，此时可能会存在并发调用CacheLoader.load()的情况
        public Builder<K, V> concurrentMaxRetry(int concurrentMaxRetry) {
            this.concurrentMaxRetry = concurrentMaxRetry;
            return this;
        }

        //并发重试时的间隔，单位ms
        public Builder<K, V> concurrentSleepMs(long concurrentSleepMs) {
            this.concurrentSleepMs = concurrentSleepMs;
            return this;
        }

        //缓存加载的接口
        public CamelliaLoadingCache<K, V> build(CacheLoader<K, V> cacheLoader) {
            if (cacheLoader == null) {
                throw new IllegalArgumentException("cacheLoader is null");
            }
            return new CamelliaLoadingCache<>(cacheLoader, initialCapacity, maxCapacity, expireMillis,
                    cacheNull, concurrentMaxRetry, concurrentSleepMs);
        }
    }

    /**
     * 获取一个值
     * 如果缓存不存在，则穿透
     * 如果缓存存在，但是超过了过期时间，则穿透
     * 缓存穿透后重新load时出现了异常，则会返回上一次的缓存结果
     * @param key 缓存key
     * @return 值
     */
    public V get(K key) {
        ValueInfo valueInfo = cacheMap.get(key);
        if (valueInfo != null) {
            if (valueInfo.isCacheNotExpire()) {
                valueInfo.updateCacheHitTime();
                return valueInfo.get();
            }
        }
        try {
            int retry = concurrentMaxRetry;
            while (retry -- > 0) {
                //避免并发缓存穿透
                boolean lock = tryLock(key);
                if (lock) {
                    try {
                        return _load(key, valueInfo);
                    } finally {
                        releaseLock(key);
                    }
                } else {
                    TimeUnit.MILLISECONDS.sleep(concurrentSleepMs);
                    if (valueInfo != null && valueInfo.isCacheNotExpire()) {
                        valueInfo.updateCacheHitTime();
                        return valueInfo.get();
                    }
                    valueInfo = cacheMap.get(key);
                    if (valueInfo != null && valueInfo.isCacheNotExpire()) {
                        valueInfo.updateCacheHitTime();
                        return valueInfo.get();
                    }
                }
            }
            return _load(key, valueInfo);
        } catch (Exception e) {
            //出现异常时返回旧值
            if (valueInfo != null) {
                return valueInfo.get();
            }
            throw new CamelliaLoadingCacheException(e);
        }
    }

    private V _load(K key, ValueInfo valueInfo) throws Exception {
        V newValue = cacheLoader.load(key);
        if (newValue != null || cacheNull) {
            if (valueInfo == null) {
                valueInfo = new ValueInfo();
            }
            valueInfo.updateValue(newValue);
            valueInfo.updateCacheRefreshTime();
            cacheMap.put(key, valueInfo);
        }
        return newValue;
    }

    private boolean tryLock(K key) {
        return getLock(key).compareAndSet(false, true);
    }

    private void releaseLock(K key) {
        getLock(key).compareAndSet(true, false);
    }

    private AtomicBoolean getLock(K key) {
        AtomicBoolean lock = lockMap.get(key);
        if (lock == null) {
            lock = new AtomicBoolean();
            AtomicBoolean old = lockMap.putIfAbsent(key, lock);
            if (old != null) {
                lock = old;
            }
        }
        return lock;
    }

    //重载缓存
    private void reload() {
        Map<K, ValueInfo> map = new HashMap<>(cacheMap);
        for (Map.Entry<K, ValueInfo> entry : map.entrySet()) {
            K key = entry.getKey();
            ValueInfo valueInfo = entry.getValue();
            if (valueInfo.isCacheHot() && valueInfo.isCacheWillExpire()) {
                try {
                    _load(key, valueInfo);
                } catch (Exception e) {
                    logger.error("reload error, key = {}", key, e);
                }
            }
        }
    }

    public interface CacheLoader<K, V> {
        V load(K key) throws Exception;
    }

    private final class ValueInfo {
        V value;
        long cacheRefreshTime;
        long cacheHitTime;

        public V get() {
            return value;
        }

        public void updateValue(V value) {
            this.value = value;
        }

        public void updateCacheRefreshTime() {
            cacheRefreshTime = now;
        }

        public void updateCacheHitTime() {
            cacheHitTime = now;
        }

        public boolean isCacheNotExpire() {
            return now < cacheRefreshTime + expireMillis;
        }

        public boolean isCacheWillExpire() {
            return now > cacheRefreshTime + expireMillis - expireMillis * 2 / 5;
        }

        public boolean isCacheHot() {
            return now - cacheHitTime < expireMillis / 2;
        }
    }
}
