package com.netease.nim.camellia.cache.spring.caffeine;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.netease.nim.camellia.cache.spring.CamelliaCacheSerializer;
import com.netease.nim.camellia.cache.spring.LocalNativeCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * 基于Caffeine
 */
public class CaffeineNativeCache extends LocalNativeCache {

    private static final Logger logger = LoggerFactory.getLogger(CaffeineNativeCache.class);

    private final Cache<String, Item> cache;
    private final boolean isSafe;
    private final CamelliaCacheSerializer<Object> serializer;

    public CaffeineNativeCache(int initialCapacity, int capacity,
                               boolean isSafe, CamelliaCacheSerializer<Object> serializer) {
        this.isSafe = isSafe;
        this.serializer = serializer;
        this.cache = Caffeine.newBuilder()
                .initialCapacity(initialCapacity).maximumSize(capacity)
                .expireAfter(new Expiry<String, Item>() {

                    private long getRestTimeInNanos(Item value) {
                        if (value.expireMillis > 0) {
                            return TimeUnit.MILLISECONDS.toNanos(value.expireMillis - System.currentTimeMillis());
                        } else {
                            return Long.MAX_VALUE;
                        }
                    }

                    @Override
                    public long expireAfterCreate(String key, Item value, long currentTime) {
                        return getRestTimeInNanos(value);
                    }

                    @Override
                    public long expireAfterUpdate(String key, Item value, long currentTime, long currentDuration) {
                        return getRestTimeInNanos(value);
                    }

                    @Override
                    public long expireAfterRead(String key, Item value, long currentTime, long currentDuration) {
                        return getRestTimeInNanos(value);
                    }
                }).build();
    }

    @Override
    public void put(String key, Object value) {
        if (isSafe) {
            cache.put(key, new Item(serializer.serialize(value), -1));
        } else {
            cache.put(key, new Item(value, -1));
        }
        if (logger.isDebugEnabled()) {
            logger.debug("put, key = {}, isSafe = {}", key, isSafe);
        }
    }

    @Override
    public void put(String key, Object value, long expireMillis) {
        if (isSafe) {
            cache.put(key, new Item(serializer.serialize(value), expireMillis));
        } else {
            cache.put(key, new Item(value, expireMillis));
        }
        if (logger.isDebugEnabled()) {
            logger.debug("put, key = {}, expireMillis = {}, isSafe = {}", key, expireMillis, isSafe);
        }
    }

    @Override
    public void multiPut(Map<String, Object> kvs) {
        if (kvs == null || kvs.isEmpty()) return;
        for (Map.Entry<String, Object> entry : kvs.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void multiPut(Map<String, Object> kvs, long expireMillis) {
        if (kvs == null || kvs.isEmpty()) return;
        for (Map.Entry<String, Object> entry : kvs.entrySet()) {
            put(entry.getKey(), entry.getValue(), expireMillis);
        }
    }

    @Override
    public Object get(String key) {
        if (logger.isDebugEnabled()) {
            logger.debug("get, key = {}, isSafe = {}", key, isSafe);
        }
        Item item = cache.getIfPresent(key);
        if (item == null || item.isExpire()) return null;
        if (isSafe) {
            return serializer.deserialize((byte[]) item.getValue());
        } else {
            return item.getValue();
        }
    }

    @Override
    public List<Object> multiGet(Collection<String> keys) {
        if (logger.isDebugEnabled()) {
            logger.debug("multiGet, keys = {}, isSafe = {}", keys, isSafe);
        }
        Map<String, Item> map = cache.getAllPresent(keys);
        List<Object> ret = new ArrayList<>();
        for (String key : keys) {
            Item item = map.get(key);
            if (item == null || item.isExpire()) {
                ret.add(null);
            } else {
                if (isSafe) {
                    ret.add(serializer.deserialize((byte[]) item.getValue()));
                } else {
                    ret.add(item.getValue());
                }
            }
        }
        return ret;
    }

    @Override
    public void delete(String key) {
        if (logger.isDebugEnabled()) {
            logger.debug("delete, key = {}, isSafe = {}", key, isSafe);
        }
        cache.invalidate(key);
    }

    @Override
    public void multiDelete(Collection<String> keys) {
        if (logger.isDebugEnabled()) {
            logger.debug("multiDelete, keys = {}, isSafe = {}", keys, isSafe);
        }
        cache.invalidateAll(keys);
    }

    @Override
    public boolean acquireLock(String key, long expireMillis) {
        PutIfAbsentFunc putIfAbsentFunc = new PutIfAbsentFunc(expireMillis);
        cache.get(key, putIfAbsentFunc);
        boolean lock = putIfAbsentFunc.called;
        if (logger.isDebugEnabled()) {
            logger.debug("acquireLock, key = {}, expireMillis = {}, lock = {}", key, expireMillis, lock);
        }
        return lock;
    }

    @Override
    public void releaseLock(String key) {
        cache.invalidate(key);
        if (logger.isDebugEnabled()) {
            logger.debug("releaseLock, key = {}", key);
        }
    }

    private static class PutIfAbsentFunc implements Function<String, Item> {
        private boolean called = false;
        private final long expireMillis;

        PutIfAbsentFunc(long expireMillis) {
            this.expireMillis = expireMillis;
        }

        @Override
        public Item apply(String key) {
            called = true;
            return new Item("", expireMillis);
        }
    }

    private static class Item {
        private long expireMillis;
        private final Object value;

        Item(Object value, long expireMillis) {
            this.value = value;
            if (expireMillis > 0) {
                this.expireMillis = System.currentTimeMillis() + expireMillis;
            }
        }

        Object getValue() {
            return value;
        }

        boolean isExpire() {
            return expireMillis > 0 && System.currentTimeMillis() > expireMillis;
        }
    }
}
