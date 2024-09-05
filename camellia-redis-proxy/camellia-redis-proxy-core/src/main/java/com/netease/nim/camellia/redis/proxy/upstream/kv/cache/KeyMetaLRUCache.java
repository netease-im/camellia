package com.netease.nim.camellia.redis.proxy.upstream.kv.cache;

import com.netease.nim.camellia.redis.proxy.cluster.ClusterModeStatus;
import com.netease.nim.camellia.redis.proxy.cluster.ProxyClusterSlotMapUtils;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.upstream.kv.conf.RedisKvConf;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.util.RedisClusterCRC16Utils;
import com.netease.nim.camellia.tools.utils.BytesKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by caojiajun on 2024/5/21
 */
public class KeyMetaLRUCache {

    private static final Logger logger = LoggerFactory.getLogger(KeyMetaLRUCache.class);

    private final String namespace;
    private int capacity;
    private SlotLRUCache<KeyMeta> localCache;
    private SlotLRUCache<Boolean> nullCache;

    public KeyMetaLRUCache(String namespace) {
        this.namespace = namespace;
        //
        rebuild();
        ProxyDynamicConf.registerCallback(this::rebuild);
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
        int capacity = RedisKvConf.getInt(namespace, "kv.key.meta.lru.cache.capacity", 50_0000);
        if (this.capacity != capacity) {
            if (localCache == null) {
                this.localCache = new SlotLRUCache<>(capacity);
            } else {
                this.localCache.setCapacity(capacity);
            }
            if (nullCache == null) {
                this.nullCache = new SlotLRUCache<>(capacity);
            } else {
                this.nullCache.setCapacity(capacity);
            }
            logger.info("key meta lru cache build, capacity = {}", capacity);
        }
        this.capacity = capacity;
    }

    public ValueWrapper<KeyMeta> get(int slot, byte[] key) {
        BytesKey bytesKey = new BytesKey(key);
        KeyMeta keyMeta = localCache.get(slot, bytesKey);
        if (keyMeta != null) {
            return () -> keyMeta;
        }
        Boolean bool = nullCache.get(slot, bytesKey);
        if (bool != null) {
            return () -> null;
        }
        return null;
    }

    public void remove(int slot, byte[] key) {
        BytesKey bytesKey = new BytesKey(key);
        localCache.remove(slot, bytesKey);
        nullCache.put(slot, bytesKey, Boolean.TRUE);
    }

    public void put(int slot, byte[] key, KeyMeta keyMeta) {
        BytesKey bytesKey = new BytesKey(key);
        localCache.put(slot, bytesKey, keyMeta);
        nullCache.remove(slot, bytesKey);
    }

    public void setNull(int slot, byte[] key) {
        BytesKey bytesKey = new BytesKey(key);
        localCache.remove(slot, bytesKey);
        nullCache.put(slot, bytesKey, Boolean.TRUE);
    }

    public void clear() {
        localCache.clear();
        nullCache.clear();
    }

    public long estimateSize() {
        long estimateSize = 0;
        estimateSize += localCache.estimateSize();
        estimateSize += nullCache.size() * 12;
        return estimateSize;
    }

}
