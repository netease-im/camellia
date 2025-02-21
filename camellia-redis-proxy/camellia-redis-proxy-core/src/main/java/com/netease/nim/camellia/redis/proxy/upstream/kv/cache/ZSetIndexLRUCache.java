package com.netease.nim.camellia.redis.proxy.upstream.kv.cache;

import com.netease.nim.camellia.redis.proxy.cluster.ClusterModeStatus;
import com.netease.nim.camellia.redis.proxy.cluster.ProxyClusterSlotMapUtils;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.monitor.KvLRUCacheMonitor;
import com.netease.nim.camellia.redis.proxy.upstream.kv.conf.RedisKvConf;
import com.netease.nim.camellia.redis.proxy.upstream.kv.utils.BytesUtils;
import com.netease.nim.camellia.tools.utils.BytesKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by caojiajun on 2024/8/15
 */
public class ZSetIndexLRUCache {

    private static final Logger logger = LoggerFactory.getLogger(ZSetIndexLRUCache.class);

    private final String namespace;
    private final SlotLRUCache<byte[]> localCache = new SlotLRUCache<>(10000);
    private final SlotLRUCache<byte[]> localCacheForWrite = new SlotLRUCache<>(10000);

    private int capacity;

    public ZSetIndexLRUCache(String namespace) {
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
                localCacheForWrite.clear(removedSlot);
            }
        });
    }

    private void rebuild() {
        int capacity = RedisKvConf.getInt(namespace, "kv.zset.index.lru.cache.capacity", -1);
        if (capacity > 0) {
            if (this.capacity != capacity) {
                this.localCache.setCapacity(capacity);
                this.localCacheForWrite.setCapacity(capacity);
                logger.info("zset index lru cache build, namespace = {}, capacity = {}", namespace, capacity);
            }
            KvLRUCacheMonitor.update(namespace, "kv.zset.index.lru.read.cache", localCache.getCapacity(), localCache.size(), localCache.estimateSize(), 0);
            KvLRUCacheMonitor.update(namespace, "kv.zset.index.lru.write.cache", localCacheForWrite.getCapacity(), localCacheForWrite.size(), localCacheForWrite.estimateSize(), 0);
            this.capacity = capacity;
        } else {
            CacheCapacityCalculator.update(localCache, namespace, "kv.zset.index.lru.read.cache");
            CacheCapacityCalculator.update(localCacheForWrite, namespace, "kv.zset.index.lru.write.cache");
            this.capacity = 0;
        }
    }

    public byte[] getForRead(int slot, byte[] cacheKey, BytesKey ref) {
        SlotCacheKey slotCacheKey = new SlotCacheKey(slot, BytesUtils.merge(cacheKey, ref.getKey()));
        byte[] bytes = localCache.get(slotCacheKey);
        if (bytes != null) {
            return bytes;
        }
        bytes = localCacheForWrite.get(slotCacheKey);
        if (bytes != null) {
            localCache.put(slotCacheKey, bytes);
            localCacheForWrite.remove(slotCacheKey);
            return bytes;
        }
        return null;
    }

    public byte[] getForWrite(int slot, byte[] cacheKey, BytesKey ref) {
        SlotCacheKey slotCacheKey = new SlotCacheKey(slot, BytesUtils.merge(cacheKey, ref.getKey()));
        byte[] bytes = localCacheForWrite.get(slotCacheKey);
        if (bytes != null) {
            return bytes;
        }
        return localCache.get(slotCacheKey);
    }

    public void putForWrite(int slot, byte[] cacheKey, BytesKey ref, byte[] raw) {
        SlotCacheKey slotCacheKey = new SlotCacheKey(slot, BytesUtils.merge(cacheKey, ref.getKey()));
        localCacheForWrite.put(slotCacheKey, raw);
    }

    public void putForRead(int slot, byte[] cacheKey, BytesKey ref, byte[] raw) {
        SlotCacheKey slotCacheKey = new SlotCacheKey(slot, BytesUtils.merge(cacheKey, ref.getKey()));
        localCache.put(slotCacheKey, raw);
    }

    public void remove(int slot, byte[] cacheKey, BytesKey ref) {
        SlotCacheKey slotCacheKey = new SlotCacheKey(slot, BytesUtils.merge(cacheKey, ref.getKey()));
        localCache.remove(slotCacheKey);
        localCacheForWrite.remove(slotCacheKey);
    }

    public void clear() {
        localCache.clear();
        localCacheForWrite.clear();
    }
}
