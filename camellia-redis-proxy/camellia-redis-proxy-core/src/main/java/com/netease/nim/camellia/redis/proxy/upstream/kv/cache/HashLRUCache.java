package com.netease.nim.camellia.redis.proxy.upstream.kv.cache;

import com.netease.nim.camellia.redis.proxy.cluster.ClusterModeStatus;
import com.netease.nim.camellia.redis.proxy.cluster.ProxyClusterSlotMapUtils;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.upstream.kv.conf.RedisKvConf;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyType;
import com.netease.nim.camellia.tools.utils.BytesKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by caojiajun on 2024/5/21
 */
public class HashLRUCache {

    private static final Logger logger = LoggerFactory.getLogger(HashLRUCache.class);

    private final String namespace;
    private final HotKeyCalculator hotKeyCalculator;

    private SlotLRUCache<RedisHash> localCache;
    private SlotLRUCache<RedisHash> localCacheForWrite;

    private int capacity;

    public HashLRUCache(String namespace) {
        this.namespace = namespace;
        this.hotKeyCalculator = new HotKeyCalculator(namespace, KeyType.hash);
        //
        rebuild();
        ProxyDynamicConf.registerCallback(this::rebuild);
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
        int capacity = RedisKvConf.getInt(namespace, "kv.hash.lru.cache.capacity", 10_0000);
        if (this.capacity != capacity) {
            if (this.localCache == null) {
                this.localCache = new SlotLRUCache<>(capacity);
            } else {
                this.localCache.setCapacity(capacity);
            }
            if (this.localCacheForWrite == null) {
                this.localCacheForWrite = new SlotLRUCache<>(capacity);
            } else {
                this.localCacheForWrite.setCapacity(capacity);
            }
            logger.info("hash lru cache build, namespace = {}, capacity = {}", namespace, capacity);
        }
        this.capacity = capacity;
    }

    public boolean isHotKey(byte[] key) {
        return hotKeyCalculator.isHotKey(key);
    }

    public void putAllForRead(int slot, byte[] cacheKey, RedisHash hash) {
        localCache.put(slot, new BytesKey(cacheKey), hash);
    }

    public void putAllForWrite(int slot, byte[] cacheKey, RedisHash hash) {
        localCacheForWrite.put(slot, new BytesKey(cacheKey), hash);
    }

    public RedisHash getForRead(int slot, byte[] cacheKey) {
        BytesKey bytesKey = new BytesKey(cacheKey);
        RedisHash hash = localCache.get(slot, bytesKey);
        if (hash == null) {
            hash = localCacheForWrite.get(slot, bytesKey);
            if (hash != null) {
                localCache.put(slot, bytesKey, hash);
                localCacheForWrite.remove(slot, bytesKey);
            }
        }
        return hash;
    }

    public RedisHash getForWrite(int slot, byte[] cacheKey) {
        BytesKey bytesKey = new BytesKey(cacheKey);
        RedisHash hash = localCache.get(slot, bytesKey);
        if (hash == null) {
            hash = localCacheForWrite.get(slot, bytesKey);
        }
        return hash;
    }

    public Map<BytesKey, byte[]> hset(int slot, byte[] cacheKey, Map<BytesKey, byte[]> fieldMap) {
        BytesKey bytesKey = new BytesKey(cacheKey);
        RedisHash hash1 = localCacheForWrite.get(slot, bytesKey);
        Map<BytesKey, byte[]> result1 = null;
        if (hash1 != null) {
            result1 = hash1.hset(fieldMap);
        }
        RedisHash hash2 = localCache.get(slot, bytesKey);
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
        BytesKey bytesKey = new BytesKey(cacheKey);
        RedisHash hash1 = localCacheForWrite.get(slot, bytesKey);
        Map<BytesKey, byte[]> result1 = null;
        if (hash1 != null) {
            result1 = hash1.hdel(fields);
        }
        RedisHash hash2 = localCache.get(slot, bytesKey);
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
        BytesKey bytesKey = new BytesKey(cacheKey);
        localCache.remove(slot, bytesKey);
        localCacheForWrite.remove(slot, bytesKey);
    }

    public void clear() {
        localCache.clear();
        localCacheForWrite.clear();
    }

    public long estimateSize() {
        long estimateSize = 0;
        estimateSize += localCache.estimateSize();
        estimateSize += localCacheForWrite.estimateSize();
        estimateSize += hotKeyCalculator.estimateSize();
        return estimateSize;
    }
}
