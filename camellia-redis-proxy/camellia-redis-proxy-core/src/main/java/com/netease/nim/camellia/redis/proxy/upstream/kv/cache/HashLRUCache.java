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
    private ConcurrentLinkedHashMap<BytesKey, Map<BytesKey, byte[]>> localCache;
    private ConcurrentLinkedHashMap<BytesKey, Map<BytesKey, byte[]>> localCacheForWrite;
    private int capacity;

    public HashLRUCache(String namespace) {
        this.namespace = namespace;
        rebuild();
        ProxyDynamicConf.registerCallback(this::rebuild);
        ClusterModeStatus.registerClusterModeSlotRefreshCallback(localCache::clear);
    }

    private void rebuild() {
        int capacity = RedisKvConf.getInt(namespace, "kv.hash.lru.cache.capacity", 100000);
        if (this.capacity != capacity) {
            if (this.localCache == null) {
                this.localCache = new ConcurrentLinkedHashMap.Builder<BytesKey, Map<BytesKey, byte[]>>()
                        .initialCapacity(capacity)
                        .maximumWeightedCapacity(capacity)
                        .build();
            } else {
                this.localCache.setCapacity(capacity);
            }
            if (this.localCacheForWrite == null) {
                this.localCacheForWrite = new ConcurrentLinkedHashMap.Builder<BytesKey, Map<BytesKey, byte[]>>()
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

    public void putAllForRead(byte[] key, Map<BytesKey, byte[]> map) {
        localCache.put(new BytesKey(key), map);
    }

    public void putAllForWrite(byte[] key, Map<BytesKey, byte[]> map) {
        localCacheForWrite.put(new BytesKey(key), map);
    }

    public Map<BytesKey, byte[]> hgetAll(byte[] key) {
        BytesKey bytesKey = new BytesKey(key);
        Map<BytesKey, byte[]> map = localCache.get(bytesKey);
        if (map == null) {
            map = localCacheForWrite.get(bytesKey);
            if (map != null) {
                localCache.put(bytesKey, map);
                localCacheForWrite.remove(bytesKey);
            }
        }
        return map;
    }

    public LRUCacheWriteResult hset(byte[] key, Map<BytesKey, byte[]> fieldMap) {
        LRUCacheWriteResult result1 = hset0(localCacheForWrite, key, fieldMap);
        LRUCacheWriteResult result2 = hset0(localCache, key, fieldMap);
        if (result1 != null) {
            return result1;
        }
        return result2;
    }

    public LRUCacheWriteResult hdel(byte[] key, Set<BytesKey> fields) {
        LRUCacheWriteResult result1 = hdel0(localCache, key, fields);
        LRUCacheWriteResult result2 = hdel0(localCacheForWrite, key, fields);
        if (result1 != null) {
            return result1;
        }
        return result2;
    }

    public long hlen(byte[] key) {
        long len = hlen0(localCache, key);
        if (len < 0) {
            len = hlen0(localCacheForWrite, key);
        }
        return len;
    }

    public ValueWrapper hget(byte[] key, byte[] field) {
        ValueWrapper valueWrapper = hget0(localCache, key, field);
        if (valueWrapper != null) {
            return valueWrapper;
        }
        return hget0(localCacheForWrite, key, field);
    }

    public void del(byte[] key) {
        BytesKey bytesKey = new BytesKey(key);
        localCache.remove(bytesKey);
        localCacheForWrite.remove(bytesKey);
    }

    public static class LRUCacheWriteResult {
        private final int influencedFields;
        private final Map<BytesKey, byte[]> cache;

        public LRUCacheWriteResult(int influencedFields, Map<BytesKey, byte[]> cache) {
            this.influencedFields = influencedFields;
            this.cache = cache;
        }

        public int getInfluencedFields() {
            return influencedFields;
        }

        public Map<BytesKey, byte[]> getCache() {
            return cache;
        }
    }

    public void clear() {
        localCache.clear();
        localCacheForWrite.clear();
    }

    private LRUCacheWriteResult hset0(ConcurrentLinkedHashMap<BytesKey, Map<BytesKey, byte[]>> cache, byte[] key, Map<BytesKey, byte[]> fieldMap) {
        Map<BytesKey, byte[]> map = cache.get(new BytesKey(key));
        if (map == null) {
            return null;
        }
        int count = 0;
        for (Map.Entry<BytesKey, byte[]> entry : fieldMap.entrySet()) {
            byte[] put = map.put(entry.getKey(), entry.getValue());
            if (put != null) {
                count ++;
            }
        }
        return new LRUCacheWriteResult(count, map);
    }

    private ValueWrapper hget0(ConcurrentLinkedHashMap<BytesKey, Map<BytesKey, byte[]>> cache, byte[] key, byte[] field) {
        Map<BytesKey, byte[]> map = cache.get(new BytesKey(key));
        if (map == null) {
            return null;
        }
        return new ValueWrapper(map.get(new BytesKey(field)));
    }

    private long hlen0(ConcurrentLinkedHashMap<BytesKey, Map<BytesKey, byte[]>> cache, byte[] key) {
        Map<BytesKey, byte[]> map = cache.get(new BytesKey(key));
        if (map == null) {
            return -1;
        }
        return map.size();
    }

    private LRUCacheWriteResult hdel0(ConcurrentLinkedHashMap<BytesKey, Map<BytesKey, byte[]>> cache, byte[] key, Set<BytesKey> fields) {
        Map<BytesKey, byte[]> map = cache.get(new BytesKey(key));
        if (map == null) {
            return null;
        }
        int count = 0;
        for (BytesKey field : fields) {
            byte[] remove = map.remove(field);
            if (remove != null) {
                count ++;
            }
        }
        return new LRUCacheWriteResult(count, map);
    }
}
