package com.netease.nim.camellia.tools.cache;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 本地缓存工具类，支持设置缓存有效期，缓存空值
 * Created by caojiajun on 2018/2/27.
 */
public class CamelliaLocalCache {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaLocalCache.class);

    private final ConcurrentLinkedHashMap<String, CacheBean> cache;

    private static final Object nullCache = new Object();

    public static CamelliaLocalCache DEFAULT = new CamelliaLocalCache();

    public CamelliaLocalCache() {
        this(100000);
    }

    public CamelliaLocalCache(int capacity) {
        this(capacity, capacity);
    }

    public CamelliaLocalCache(int initialCapacity, int capacity) {
        cache = new ConcurrentLinkedHashMap.Builder<String, CacheBean>()
                .initialCapacity(initialCapacity).maximumWeightedCapacity(capacity).build();
    }

    /**
     * 添加缓存
     */
    public void put(String tag, Object key, Object value, long expireMillis) {
        String uniqueKey = buildCacheKey(tag, key);
        CacheBean bean;
        if (value == null) {
            value = nullCache;
        }
        if (expireMillis <= 0) {
            bean = new CacheBean(value, Long.MAX_VALUE);
        } else {
            bean = new CacheBean(value, System.currentTimeMillis() + expireMillis);
        }
        cache.put(uniqueKey, bean);
        if (logger.isDebugEnabled()) {
            logger.debug("local cache put, tag = {}, key = {}, expireMillis = {}", tag, key, expireMillis);
        }
    }

    /**
     * 添加缓存
     */
    public void put(String tag, Object key, Object value, int expireSeconds) {
        String uniqueKey = buildCacheKey(tag, key);
        CacheBean bean;
        if (value == null) {
            value = nullCache;
        }
        if (expireSeconds <= 0) {
            bean = new CacheBean(value, Long.MAX_VALUE);
        } else {
            bean = new CacheBean(value, System.currentTimeMillis() + expireSeconds * 1000);
        }
        cache.put(uniqueKey, bean);
        if (logger.isDebugEnabled()) {
            logger.debug("local cache put, tag = {}, key = {}, expireSeconds = {}", tag, key, expireSeconds);
        }
    }

    /**
     * 添加缓存（检查是否第一次）
     */
    public boolean putIfAbsent(String tag, Object key, Object value, int expireSeconds) {
        String uniqueKey = buildCacheKey(tag, key);
        CacheBean bean;
        if (value == null) {
            value = nullCache;
        }
        if (expireSeconds <= 0) {
            bean = new CacheBean(value, Long.MAX_VALUE);
        } else {
            bean = new CacheBean(value, System.currentTimeMillis() + expireSeconds * 1000);
        }
        CacheBean oldBean = cache.get(uniqueKey);
        if (oldBean != null && oldBean.isExpire()) {
            cache.remove(uniqueKey);
        }
        CacheBean cacheBean = cache.putIfAbsent(uniqueKey, bean);
        if (logger.isDebugEnabled()) {
            logger.debug("local cache put, tag = {}, key = {}, expireSeconds = {}", tag, key, expireSeconds);
        }
        return cacheBean == null;
    }

    /**
     * 获取缓存
     */
    public ValueWrapper get(String tag, Object key) {
        String uniqueKey = buildCacheKey(tag, key);
        final CacheBean bean = cache.get(uniqueKey);
        ValueWrapper ret = null;
        if (bean != null) {
            if (bean.isExpire()) {
                cache.remove(uniqueKey);
            } else if (isNullCache(bean.getValue())) {
                ret = () -> null;
            } else {
                ret = bean::getValue;
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("local cache get, tag = {}, key = {}, hit = {}", tag, key, ret != null);
        }
        return ret;
    }

    /**
     * 获取缓存
     */
    public <T> T get(String tag, Object key, Class<T> clazz) {
        String uniqueKey = buildCacheKey(tag, key);
        CacheBean bean = cache.get(uniqueKey);
        T ret = null;
        if (bean != null) {
            if (bean.isExpire()) {
                cache.remove(uniqueKey);
            } else if (isNullCache(bean.getValue())) {
                ret = null;
            } else if (clazz.isAssignableFrom(bean.getValue().getClass())) {
                ret = (T) bean.getValue();
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("local cache get, tag = {}, key = {}, class = {}, hit = {}",
                    tag, key, clazz.getSimpleName(), ret != null);
        }
        return ret;
    }

    /**
     * 删除缓存
     */
    public void evict(String tag, Object key) {
        String uniqueKey = buildCacheKey(tag, key);
        cache.remove(uniqueKey);
        if (logger.isDebugEnabled()) {
            logger.debug("local cache evict, tag = {}, key = {}", tag, key);
        }
    }

    /**
     * 清空缓存
     */
    public void clear() {
        if (!cache.isEmpty()) {
            cache.clear();
        }
        if (logger.isDebugEnabled()) {
            logger.debug("local cache clear");
        }
    }

    //是否缓存了NULL值
    private boolean isNullCache(Object value) {
        return value != null && value.equals(nullCache);
    }

    private String buildCacheKey(String tag, Object...obj) {
        StringBuilder key = new StringBuilder(tag);
        if (obj != null) {
            key.append("|");
            for (int i=0; i<obj.length; i++) {
                if (i == obj.length - 1) {
                    key.append(obj[i]);
                } else {
                    key.append(obj[i]).append("|");
                }
            }
        }
        return key.toString();
    }

    private static class CacheBean {
        private final long expireTime;
        private final Object value;

        CacheBean(Object value, long expireTime) {
            this.value = value;
            this.expireTime = expireTime;
        }

        Object getValue() {
            return value;
        }

        boolean isExpire() {
            return System.currentTimeMillis() > expireTime;
        }
    }

    public interface ValueWrapper {

        /**
         * Return the actual value in the cache.
         */
        Object get();
    }

}
