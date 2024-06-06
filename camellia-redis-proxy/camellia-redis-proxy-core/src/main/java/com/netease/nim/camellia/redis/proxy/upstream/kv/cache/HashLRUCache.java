package com.netease.nim.camellia.redis.proxy.upstream.kv.cache;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.netease.nim.camellia.redis.proxy.cluster.ClusterModeStatus;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.upstream.kv.conf.RedisKvConf;
import com.netease.nim.camellia.tools.utils.BytesKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

/**
 * Created by caojiajun on 2024/5/21
 */
public class HashLRUCache {

    private static final Logger logger = LoggerFactory.getLogger(HashLRUCache.class);

    private final String namespace;
    private ConcurrentLinkedHashMap<BytesKey, Hash> localCache;
    private ConcurrentLinkedHashMap<BytesKey, Hash> localCacheForWrite;
    private int capacity;

    public HashLRUCache(String namespace) {
        this.namespace = namespace;
        rebuild();
        ProxyDynamicConf.registerCallback(this::rebuild);
        ClusterModeStatus.registerClusterModeSlotRefreshCallback(localCache::clear);
    }

    private void rebuild() {
        int capacity = RedisKvConf.getInt(namespace, "kv.hash.lru.cache.capacity", 500000);
        if (this.capacity != capacity) {
            if (this.localCache == null) {
                this.localCache = new ConcurrentLinkedHashMap.Builder<BytesKey, Hash>()
                        .initialCapacity(capacity)
                        .maximumWeightedCapacity(capacity)
                        .build();
            } else {
                this.localCache.setCapacity(capacity);
            }
            if (this.localCacheForWrite == null) {
                this.localCacheForWrite = new ConcurrentLinkedHashMap.Builder<BytesKey, Hash>()
                        .initialCapacity(capacity)
                        .maximumWeightedCapacity(capacity)
                        .build();
            } else {
                this.localCacheForWrite.setCapacity(capacity);
            }
            logger.info("hash lru cache build, capacity = {}", capacity);
        }
        this.capacity = capacity;
    }

    public void putAllForRead(byte[] cacheKey, Hash hash) {
        localCache.put(new BytesKey(cacheKey), hash);
    }

    public void putAllForWrite(byte[] cacheKey, Hash hash) {
        localCacheForWrite.put(new BytesKey(cacheKey), hash);
    }

    public Hash get(byte[] cacheKey) {
        BytesKey bytesKey = new BytesKey(cacheKey);
        Hash hash = localCache.get(bytesKey);
        if (hash == null) {
            hash = localCacheForWrite.get(bytesKey);
            if (hash != null) {
                localCache.put(bytesKey, hash);
                localCacheForWrite.remove(bytesKey);
            }
        }
        return hash;
    }

    public Map<BytesKey, byte[]> hset(byte[] cacheKey, Map<BytesKey, byte[]> fieldMap) {
        BytesKey bytesKey = new BytesKey(cacheKey);
        Hash hash1 = localCacheForWrite.get(bytesKey);
        Map<BytesKey, byte[]> result1 = null;
        if (hash1 != null) {
            result1 = hash1.hset(fieldMap);
        }
        Hash hash2 = localCache.get(bytesKey);
        Map<BytesKey, byte[]> result2 = null;
        if (hash2 != null) {
            result2 = hash2.hset(fieldMap);
        }
        if (result1 != null) {
            return result1;
        }
        return result2;
    }

    public Map<BytesKey, byte[]> hdel(byte[] cacheKey, Set<BytesKey> fields) {
        BytesKey bytesKey = new BytesKey(cacheKey);
        Hash hash1 = localCacheForWrite.get(bytesKey);
        Map<BytesKey, byte[]> result1 = null;
        if (hash1 != null) {
            result1 = hash1.hdel(fields);
        }
        Hash hash2 = localCache.get(bytesKey);
        Map<BytesKey, byte[]> result2 = null;
        if (hash2 != null) {
            result2 = hash2.hdel(fields);
        }
        if (result1 != null) {
            return result1;
        }
        return result2;
    }

    public void del(byte[] cacheKey) {
        BytesKey bytesKey = new BytesKey(cacheKey);
        localCache.remove(bytesKey);
        localCacheForWrite.remove(bytesKey);
    }

    public void clear() {
        localCache.clear();
        localCacheForWrite.clear();
    }
}
