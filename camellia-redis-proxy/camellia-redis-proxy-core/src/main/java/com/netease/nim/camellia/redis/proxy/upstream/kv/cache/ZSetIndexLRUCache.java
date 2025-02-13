package com.netease.nim.camellia.redis.proxy.upstream.kv.cache;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.redis.proxy.cluster.ClusterModeStatus;
import com.netease.nim.camellia.redis.proxy.cluster.ProxyClusterSlotMapUtils;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.upstream.kv.conf.RedisKvConf;
import com.netease.nim.camellia.redis.proxy.upstream.kv.utils.BytesUtils;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import com.netease.nim.camellia.tools.utils.BytesKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by caojiajun on 2024/8/15
 */
public class ZSetIndexLRUCache {

    private static final Logger logger = LoggerFactory.getLogger(ZSetIndexLRUCache.class);

    private final String namespace;

    private int capacity;

    private SlotLRUCache<byte[]> localCache;
    private SlotLRUCache<byte[]> localCacheForWrite;

    public ZSetIndexLRUCache(String namespace) {
        this.namespace = namespace;
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
        int capacity = RedisKvConf.getInt(namespace, "kv.zset.index.lru.cache.capacity", 10_0000);
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
            logger.info("zset index lru cache build, namespace = {}, capacity = {}", namespace, capacity);
        }
        this.capacity = capacity;
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

    public JSONObject info() {
        JSONObject json = new JSONObject(true);
        long total = 0;
        {
            long estimateSize = localCache.estimateSize();
            total += estimateSize;
            json.put("read.cache.estimate.size", Utils.humanReadableByteCountBin(estimateSize));
            json.put("read.cache.key.count", localCache.size());
            json.put("read.cache.key.capacity", localCache.getCapacity());
        }
        {
            long estimateSize = localCacheForWrite.estimateSize();
            total += estimateSize;
            json.put("write.cache.estimate.size", Utils.humanReadableByteCountBin(estimateSize));
            json.put("write.cache.key.count", localCacheForWrite.size());
            json.put("write.cache.key.capacity", localCacheForWrite.getCapacity());
        }
        {
            json.put("total.estimate.size", Utils.humanReadableByteCountBin(total));
        }
        return json;
    }
}
