package com.netease.nim.camellia.cache.spring.redis;

import com.netease.nim.camellia.cache.core.CamelliaCacheEnv;
import com.netease.nim.camellia.cache.spring.CamelliaCacheSerializer;
import com.netease.nim.camellia.cache.spring.CamelliaCacheSerializerException;
import com.netease.nim.camellia.cache.spring.RemoteNativeCache;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import com.netease.nim.camellia.redis.pipeline.ICamelliaRedisPipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.util.SafeEncoder;

import java.util.*;

public class RedisNativeCache extends RemoteNativeCache {

    private static final Logger logger = LoggerFactory.getLogger(RedisNativeCache.class);

    private final CamelliaRedisTemplate template;
    private final CamelliaCacheSerializer<Object> serializer;

    public RedisNativeCache(CamelliaRedisTemplate template, CamelliaCacheSerializer<Object> serializer) {
        this.template = template;
        this.serializer = serializer;
    }

    @Override
    public void put(String key, Object value) {
        byte[] raw = serializer.serialize(value);
        if (raw.length > CamelliaCacheEnv.maxCacheValue) {
            logger.warn("cache value.length[{}] exceed threshold[{}], key = {}", raw.length, CamelliaCacheEnv.maxCacheValue, key);
            return;
        }
        template.set(SafeEncoder.encode(key), raw);
        if (logger.isDebugEnabled()) {
            logger.debug("put, key = {}", key);
        }
    }

    @Override
    public void put(String key, Object value, long expireMillis) {
        byte[] raw = serializer.serialize(value);
        if (raw.length > CamelliaCacheEnv.maxCacheValue) {
            logger.warn("cache value.length[{}] exceed threshold[{}], key = {}", raw.length, CamelliaCacheEnv.maxCacheValue, key);
            return;
        }
        template.psetex(SafeEncoder.encode(key), expireMillis, raw);
        if (logger.isDebugEnabled()) {
            logger.debug("put, key = {}, expireMillis = {}", key, expireMillis);
        }
    }

    @Override
    public void multiPut(Map<String, Object> kvs) {
        if (kvs == null || kvs.isEmpty()) return;
        List<byte[]> list = new ArrayList<>(kvs.size()*2);
        for (Map.Entry<String, Object> entry : kvs.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            byte[] raw = serializer.serialize(value);
            if (raw.length > CamelliaCacheEnv.maxCacheValue) {
                logger.warn("cache value.length[{}] exceed threshold[{}], key = {}", raw.length, CamelliaCacheEnv.maxCacheValue, key);
                continue;
            }
            list.add(SafeEncoder.encode(key));
            list.add(raw);
        }
        template.mset(list.toArray(new byte[0][0]));
        if (logger.isDebugEnabled()) {
            logger.debug("multiPut, keys = {}", kvs.keySet());
        }
    }

    @Override
    public void multiPut(Map<String, Object> kvs, long expireMillis) {
        if (kvs == null || kvs.isEmpty()) return;
        try (ICamelliaRedisPipeline pipelined = template.pipelined()) {
            for (Map.Entry<String, Object> entry : kvs.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                byte[] raw = serializer.serialize(value);
                if (raw.length > CamelliaCacheEnv.maxCacheValue) {
                    logger.warn("cache value.length[{}] exceed threshold[{}], key = {}", raw.length, CamelliaCacheEnv.maxCacheValue, key);
                    continue;
                }
                pipelined.psetex(SafeEncoder.encode(key), expireMillis, raw);
            }
            pipelined.sync();
        } finally {
            if (logger.isDebugEnabled()) {
                logger.debug("multiPut, keys = {}, expireMillis = {}", kvs.keySet(), expireMillis);
            }
        }
    }

    @Override
    public Object get(String key) {
        byte[] bytes = template.get(SafeEncoder.encode(key));
        if (bytes == null) return null;
        try {
            return serializer.deserialize(bytes);
        } catch (CamelliaCacheSerializerException e) {
            logger.error("deserialize error", e);
            template.del(key);
            return null;
        } finally {
            if (logger.isDebugEnabled()) {
                logger.debug("get, key = {}", key);
            }
        }
    }

    @Override
    public List<Object> multiGet(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) return Collections.emptyList();
        List<byte[]> list = new ArrayList<>(keys.size());
        for (String key : keys) {
            list.add(SafeEncoder.encode(key));
        }
        List<byte[]> values = template.mget(list.toArray(new byte[0][0]));
        List<Object> ret = new ArrayList<>();
        int index = 0;
        for (byte[] value : values) {
            if (value == null) {
                ret.add(null);
            } else {
                try {
                    ret.add(serializer.deserialize(value));
                } catch (CamelliaCacheSerializerException e) {
                    logger.error("deserialize error", e);
                    template.del(list.get(index));
                    ret.add(null);
                }
            }
            index ++;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("multiGet, keys = {}", keys);
        }
        return ret;
    }

    @Override
    public void delete(String key) {
        template.del(key);
        if (logger.isDebugEnabled()) {
            logger.debug("delete, key = {}", key);
        }
    }

    @Override
    public void multiDelete(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) return;
        List<byte[]> list = new ArrayList<>(keys.size());
        for (String key : keys) {
            list.add(SafeEncoder.encode(key));
        }
        template.del(list.toArray(new byte[0][0]));
        if (logger.isDebugEnabled()) {
            logger.debug("multiDelete, keys = {}", keys);
        }
    }

    @Override
    public boolean acquireLock(String key, long expireMillis) {
        String value = UUID.randomUUID().toString();
        Long ret = template.setnx(key, value);
        boolean lock = ret > 0;
        if (lock) {
            template.pexpire(key, expireMillis);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("acquireLock, key = {}, expireMillis = {}, lock = {}", key, expireMillis, lock);
        }
        return lock;
    }

    @Override
    public void releaseLock(String key) {
        template.del(key);
        if (logger.isDebugEnabled()) {
            logger.debug("releaseLock, key = {}", key);
        }
    }
}
