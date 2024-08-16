package com.netease.nim.camellia.redis.proxy.upstream.kv.cache;

import com.netease.nim.camellia.redis.proxy.cluster.ClusterModeStatus;
import com.netease.nim.camellia.redis.proxy.cluster.ProxyClusterSlotMapUtils;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.upstream.kv.conf.RedisKvConf;
import com.netease.nim.camellia.redis.proxy.upstream.kv.utils.BytesUtils;
import com.netease.nim.camellia.redis.proxy.util.RedisClusterCRC16Utils;
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
            logger.info("zset index lru cache build, namespace = {}, capacity = {}", namespace, capacity);
        }
        this.capacity = capacity;
    }

    public byte[] get(byte[] key, byte[] cacheKey, BytesKey ref) {
        int slot = RedisClusterCRC16Utils.getSlot(key);
        BytesKey indexCacheKey = new BytesKey(BytesUtils.merge(cacheKey, ref.getKey()));
        return localCache.get(slot, indexCacheKey);
    }

    public void put(byte[] key, byte[] cacheKey, BytesKey ref, byte[] raw) {
        int slot = RedisClusterCRC16Utils.getSlot(key);
        BytesKey indexCacheKey = new BytesKey(BytesUtils.merge(cacheKey, ref.getKey()));
        localCache.put(slot, indexCacheKey, raw);
    }

    public void remove(byte[] key, byte[] cacheKey, BytesKey ref) {
        int slot = RedisClusterCRC16Utils.getSlot(key);
        BytesKey indexCacheKey = new BytesKey(BytesUtils.merge(cacheKey, ref.getKey()));
        localCache.remove(slot, indexCacheKey);
    }

    public void clear() {
        localCache.clear();
    }
}
