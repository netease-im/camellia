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
            if (this.localCache != null) {
                this.localCache.clear();
            }
            this.localCache = new ConcurrentLinkedHashMap.Builder<BytesKey, Map<BytesKey, byte[]>>()
                    .initialCapacity(capacity)
                    .maximumWeightedCapacity(capacity)
                    .build();
            logger.info("hash lru cache build, capacity = {}", capacity);
        }
        this.capacity = capacity;
    }

    public void putAll(byte[] key, Map<BytesKey, byte[]> map) {
        localCache.put(new BytesKey(key), map);
    }

    public Map<BytesKey, byte[]> hgetAll(byte[] key) {
        return localCache.get(new BytesKey(key));
    }

    public LRUCacheWriteResult hset(byte[] key, Map<BytesKey, byte[]> fieldMap) {
        Map<BytesKey, byte[]> map = localCache.get(new BytesKey(key));
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

    public LRUCacheWriteResult hdel(byte[] key, Set<BytesKey> fields) {
        Map<BytesKey, byte[]> map = localCache.get(new BytesKey(key));
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

    public long hlen(byte[] key) {
        Map<BytesKey, byte[]> map = localCache.get(new BytesKey(key));
        if (map == null) {
            return -1;
        }
        return map.size();
    }

    public ValueWrapper hget(byte[] key, byte[] field) {
        Map<BytesKey, byte[]> map = localCache.get(new BytesKey(key));
        if (map == null) {
            return null;
        }
        return new ValueWrapper(map.get(new BytesKey(field)));
    }

    public void del(byte[] key) {
        localCache.remove(new BytesKey(key));
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
    }

}
