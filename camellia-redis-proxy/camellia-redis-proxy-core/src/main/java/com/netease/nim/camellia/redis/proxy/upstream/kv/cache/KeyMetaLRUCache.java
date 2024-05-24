package com.netease.nim.camellia.redis.proxy.upstream.kv.cache;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.netease.nim.camellia.redis.proxy.cluster.ClusterModeStatus;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.upstream.kv.conf.RedisKvConf;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;
import com.netease.nim.camellia.tools.utils.BytesKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by caojiajun on 2024/5/21
 */
public class KeyMetaLRUCache {

    private static final Logger logger = LoggerFactory.getLogger(KeyMetaLRUCache.class);

    private final String namespace;
    private int capacity;
    private ConcurrentLinkedHashMap<BytesKey, KeyMeta> localCache;

    public KeyMetaLRUCache(String namespace) {
        this.namespace = namespace;
        rebuild();
        ProxyDynamicConf.registerCallback(this::rebuild);
        ClusterModeStatus.registerClusterModeSlotRefreshCallback(localCache::clear);
    }

    private void rebuild() {
        int capacity = RedisKvConf.getInt(namespace, "kv.key.meta.lru.cache.capacity", 100000);
        if (this.capacity != capacity) {
            if (this.localCache != null) {
                this.localCache.clear();
            }
            this.localCache = new ConcurrentLinkedHashMap.Builder<BytesKey, KeyMeta>()
                    .initialCapacity(capacity)
                    .maximumWeightedCapacity(capacity)
                    .build();
            logger.info("key meta lru cache build, capacity = {}", capacity);
        }
        this.capacity = capacity;
    }

    public KeyMeta get(byte[] key) {
        return localCache.get(new BytesKey(key));
    }

    public void remove(byte[] key) {
        localCache.remove(new BytesKey(key));
    }

    public void put(byte[] key, KeyMeta keyMeta) {
        localCache.put(new BytesKey(key), keyMeta);
    }

    public void clear() {
        localCache.clear();
    }
}
