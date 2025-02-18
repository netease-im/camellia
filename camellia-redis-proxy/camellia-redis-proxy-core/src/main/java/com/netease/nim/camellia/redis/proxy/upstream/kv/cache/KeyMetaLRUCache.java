package com.netease.nim.camellia.redis.proxy.upstream.kv.cache;

import com.netease.nim.camellia.redis.proxy.cluster.ClusterModeStatus;
import com.netease.nim.camellia.redis.proxy.cluster.ProxyClusterSlotMapUtils;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.upstream.kv.conf.RedisKvConf;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by caojiajun on 2024/5/21
 */
public class KeyMetaLRUCache {

    private static final Logger logger = LoggerFactory.getLogger(KeyMetaLRUCache.class);

    private final String namespace;
    private final SlotLRUCache<KeyMeta> localCache = new SlotLRUCache<>(10000);
    private final SlotLRUCache<Boolean> nullCache = new SlotLRUCache<>(10000);

    private int capacity;

    public KeyMetaLRUCache(String namespace) {
        this.namespace = namespace;
        //
        rebuild();
        ProxyDynamicConf.registerCallback(this::rebuild);
        CacheCapacityCalculator.scheduleAtFixedRate(this::rebuild, 10, 10, TimeUnit.SECONDS);
        //
        ClusterModeStatus.registerClusterSlotMapChangeCallback((oldSlotMap, newSlotMap) -> {
            List<Integer> removedSlots = ProxyClusterSlotMapUtils.removedSlots(oldSlotMap, newSlotMap);
            for (Integer removedSlot : removedSlots) {
                localCache.clear(removedSlot);
                nullCache.clear(removedSlot);
            }
        });
    }

    private void rebuild() {
        int capacity = RedisKvConf.getInt(namespace, "kv.key.meta.lru.cache.capacity", -1);
        if (capacity > 0) {
            if (this.capacity != capacity) {
                this.localCache.setCapacity(capacity);
                this.nullCache.setCapacity(capacity);
                logger.info("key meta lru cache build, capacity = {}", capacity);
            }
            this.capacity = capacity;
        } else {
            CacheCapacityCalculator.update(localCache, namespace, "kv.key.meta.lru.cache");
            CacheCapacityCalculator.update(nullCache, namespace, "kv.key.meta.lru.null.cache");
        }
    }

    public ValueWrapper<KeyMeta> get(int slot, byte[] key) {
        SlotCacheKey slotCacheKey = new SlotCacheKey(slot, key);
        KeyMeta keyMeta = localCache.get(slotCacheKey);
        if (keyMeta != null) {
            return () -> keyMeta;
        }
        Boolean bool = nullCache.get(slotCacheKey);
        if (bool != null) {
            return () -> null;
        }
        return null;
    }

    public void remove(int slot, byte[] key) {
        SlotCacheKey slotCacheKey = new SlotCacheKey(slot, key);
        localCache.remove(slotCacheKey);
        nullCache.put(slotCacheKey, Boolean.TRUE);
    }

    public void put(int slot, byte[] key, KeyMeta keyMeta) {
        SlotCacheKey slotCacheKey = new SlotCacheKey(slot, key);
        localCache.put(slotCacheKey, keyMeta);
        nullCache.remove(slotCacheKey);
    }

    public void setNull(int slot, byte[] key) {
        SlotCacheKey slotCacheKey = new SlotCacheKey(slot, key);
        localCache.remove(slotCacheKey);
        nullCache.put(slotCacheKey, Boolean.TRUE);
    }

    public void clear() {
        localCache.clear();
        nullCache.clear();
    }

}
