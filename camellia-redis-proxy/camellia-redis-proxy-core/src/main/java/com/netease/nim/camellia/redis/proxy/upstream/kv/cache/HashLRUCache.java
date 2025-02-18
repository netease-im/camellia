package com.netease.nim.camellia.redis.proxy.upstream.kv.cache;

import com.netease.nim.camellia.redis.proxy.cluster.ClusterModeStatus;
import com.netease.nim.camellia.redis.proxy.cluster.ProxyClusterSlotMapUtils;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.upstream.kv.conf.RedisKvConf;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyType;
import com.netease.nim.camellia.tools.utils.BytesKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Created by caojiajun on 2024/5/21
 */
public class HashLRUCache {

    private static final Logger logger = LoggerFactory.getLogger(HashLRUCache.class);

    private final String namespace;
    private final HotKeyCalculator hotKeyCalculator;
    private final SlotLRUCache<RedisHash> localCache = new SlotLRUCache<>(10000);
    private final SlotLRUCache<RedisHash> localCacheForWrite = new SlotLRUCache<>(10000);

    private int capacity;

    public HashLRUCache(String namespace) {
        this.namespace = namespace;
        this.hotKeyCalculator = new HotKeyCalculator(namespace, KeyType.hash);
        //
        rebuild();
        ProxyDynamicConf.registerCallback(this::rebuild);
        CacheCapacityCalculator.scheduleAtFixedRate(this::rebuild, 10, 10, TimeUnit.SECONDS);
        //
        ClusterModeStatus.registerClusterSlotMapChangeCallback((oldSlotMap, newSlotMap) -> {
            List<Integer> removedSlots = ProxyClusterSlotMapUtils.removedSlots(oldSlotMap, newSlotMap);
            for (Integer removedSlot : removedSlots) {
                localCache.clear(removedSlot);
                localCacheForWrite.clear(removedSlot);
            }
        });
    }

    private void rebuild() {
        int capacity = RedisKvConf.getInt(namespace, "kv.hash.lru.cache.capacity", -1);
        if (capacity > 0) {
            if (this.capacity != capacity) {
                this.localCache.setCapacity(capacity);
                this.localCacheForWrite.setCapacity(capacity);
                logger.info("hash lru cache build, namespace = {}, capacity = {}", namespace, capacity);
            }
            this.capacity = capacity;
        } else {
            CacheCapacityCalculator.update(localCache, namespace, "kv.hash.lru.read.cache");
            CacheCapacityCalculator.update(localCacheForWrite, namespace, "kv.hash.lru.write.cache");
        }
    }

    public boolean isHotKey(byte[] key, RedisCommand redisCommand) {
        return hotKeyCalculator.isHotKey(key, redisCommand);
    }

    public void putAllForRead(int slot, byte[] cacheKey, RedisHash hash) {
        SlotCacheKey slotCacheKey = new SlotCacheKey(slot, cacheKey);
        localCache.put(slotCacheKey, hash);
    }

    public void putAllForWrite(int slot, byte[] cacheKey, RedisHash hash) {
        SlotCacheKey slotCacheKey = new SlotCacheKey(slot, cacheKey);
        localCacheForWrite.put(slotCacheKey, hash);
    }

    public RedisHash getForRead(int slot, byte[] cacheKey) {
        SlotCacheKey slotCacheKey = new SlotCacheKey(slot, cacheKey);
        RedisHash hash = localCache.get(slotCacheKey);
        if (hash == null) {
            hash = localCacheForWrite.get(slotCacheKey);
            if (hash != null) {
                localCache.put(slotCacheKey, hash);
                localCacheForWrite.remove(slotCacheKey);
            }
        }
        return hash;
    }

    public RedisHash getForWrite(int slot, byte[] cacheKey) {
        SlotCacheKey slotCacheKey = new SlotCacheKey(slot, cacheKey);
        RedisHash hash = localCache.get(slotCacheKey);
        if (hash == null) {
            hash = localCacheForWrite.get(slotCacheKey);
        }
        return hash;
    }

    public Map<BytesKey, byte[]> hset(int slot, byte[] cacheKey, Map<BytesKey, byte[]> fieldMap) {
        SlotCacheKey slotCacheKey = new SlotCacheKey(slot, cacheKey);
        RedisHash hash1 = localCacheForWrite.get(slotCacheKey);
        Map<BytesKey, byte[]> result1 = null;
        if (hash1 != null) {
            result1 = hash1.hset(fieldMap);
        }
        RedisHash hash2 = localCache.get(slotCacheKey);
        Map<BytesKey, byte[]> result2 = null;
        if (hash2 != null) {
            result2 = hash2.hset(fieldMap);
        }
        if (result1 != null) {
            return result1;
        }
        return result2;
    }

    public Map<BytesKey, byte[]> hdel(int slot, byte[] cacheKey, Set<BytesKey> fields) {
        SlotCacheKey slotCacheKey = new SlotCacheKey(slot, cacheKey);
        RedisHash hash1 = localCacheForWrite.get(slotCacheKey);
        Map<BytesKey, byte[]> result1 = null;
        if (hash1 != null) {
            result1 = hash1.hdel(fields);
        }
        RedisHash hash2 = localCache.get(slotCacheKey);
        Map<BytesKey, byte[]> result2 = null;
        if (hash2 != null) {
            result2 = hash2.hdel(fields);
        }
        if (result1 != null) {
            return result1;
        }
        return result2;
    }

    public void del(int slot, byte[] cacheKey) {
        SlotCacheKey slotCacheKey = new SlotCacheKey(slot, cacheKey);
        localCache.remove(slotCacheKey);
        localCacheForWrite.remove(slotCacheKey);
    }

    public void clear() {
        localCache.clear();
        localCacheForWrite.clear();
    }
}
