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
    private SlotLRUCache<Boolean> emptyCache;

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
            }
        });
    }

    private void rebuild() {
        int capacity = RedisKvConf.getInt(namespace, "kv.key.meta.lru.cache.capacity", 500000);
        if (this.capacity != capacity) {
            if (localCache == null) {
                this.localCache = new SlotLRUCache<>(capacity);
            } else {
                this.localCache.setCapacity(capacity);
            }
            if (emptyCache == null) {
                this.emptyCache = new SlotLRUCache<>(capacity);
            } else {
                this.emptyCache.setCapacity(capacity);
            }
            logger.info("key meta lru cache build, capacity = {}", capacity);
        }
        this.capacity = capacity;
    }

    public ValueWrapper<KeyMeta> get(byte[] key) {
        int slot = RedisClusterCRC16Utils.getSlot(key);
        BytesKey bytesKey = new BytesKey(key);
        KeyMeta keyMeta = localCache.get(slot, bytesKey);
        if (keyMeta != null) {
            return () -> keyMeta;
        }
        Boolean bool = emptyCache.get(slot, bytesKey);
        if (bool != null) {
            return () -> null;
        }
        return null;
    }

    public void remove(byte[] key) {
        int slot = RedisClusterCRC16Utils.getSlot(key);
        BytesKey bytesKey = new BytesKey(key);
        localCache.remove(slot, bytesKey);
        emptyCache.put(slot, bytesKey, Boolean.TRUE);
    }

    public void put(byte[] key, KeyMeta keyMeta) {
        int slot = RedisClusterCRC16Utils.getSlot(key);
        BytesKey bytesKey = new BytesKey(key);
        localCache.put(slot, bytesKey, keyMeta);
        emptyCache.remove(slot, bytesKey);
    }

    public void setNull(byte[] key) {
        int slot = RedisClusterCRC16Utils.getSlot(key);
        BytesKey bytesKey = new BytesKey(key);
        localCache.remove(slot, bytesKey);
        emptyCache.put(slot, bytesKey, Boolean.TRUE);
    }

    public void clear() {
        localCache.clear();
        emptyCache.clear();
    }

}
