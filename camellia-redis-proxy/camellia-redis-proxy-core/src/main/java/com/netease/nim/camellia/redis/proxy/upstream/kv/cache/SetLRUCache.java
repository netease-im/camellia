package com.netease.nim.camellia.redis.proxy.upstream.kv.cache;

import com.netease.nim.camellia.redis.proxy.cluster.ClusterModeStatus;
import com.netease.nim.camellia.redis.proxy.cluster.ProxyClusterSlotMapUtils;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.upstream.kv.conf.RedisKvConf;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyType;
import com.netease.nim.camellia.redis.proxy.util.RedisClusterCRC16Utils;
import com.netease.nim.camellia.tools.utils.BytesKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Created by caojiajun on 2024/5/21
 */
public class SetLRUCache {

    private static final Logger logger = LoggerFactory.getLogger(SetLRUCache.class);

    private final String namespace;
    private final HotKeyCalculator hotKeyCalculator;

    private SlotLRUCache<RedisSet> localCache;
    private SlotLRUCache<RedisSet> localCacheForWrite;

    private int capacity;

    public SetLRUCache(String namespace) {
        this.namespace = namespace;
        this.hotKeyCalculator = new HotKeyCalculator(namespace, KeyType.set);
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
        int capacity = RedisKvConf.getInt(namespace, "kv.set.lru.cache.capacity", 10_0000);
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
            logger.info("set lru cache build, namespace = {}, capacity = {}", namespace, capacity);
        }
        this.capacity = capacity;
    }

    public boolean isHotKey(byte[] key) {
        return hotKeyCalculator.isHotKey(key);
    }

    public void putAllForRead(byte[] key, byte[] cacheKey, RedisSet set) {
        int slot = RedisClusterCRC16Utils.getSlot(key);
        localCache.put(slot, new BytesKey(cacheKey), set);
    }

    public void putAllForWrite(byte[] key, byte[] cacheKey, RedisSet set) {
        int slot = RedisClusterCRC16Utils.getSlot(key);
        localCacheForWrite.put(slot, new BytesKey(cacheKey), set);
    }

    public RedisSet getForRead(byte[] key, byte[] cacheKey) {
        int slot = RedisClusterCRC16Utils.getSlot(key);
        BytesKey bytesKey = new BytesKey(cacheKey);
        RedisSet set = localCache.get(slot, bytesKey);
        if (set == null) {
            set = localCacheForWrite.get(slot, bytesKey);
            if (set != null) {
                localCache.put(slot, bytesKey, set);
                localCacheForWrite.remove(slot, bytesKey);
            }
        }
        return set;
    }

    public RedisSet getForWrite(byte[] key, byte[] cacheKey) {
        int slot = RedisClusterCRC16Utils.getSlot(key);
        BytesKey bytesKey = new BytesKey(cacheKey);
        RedisSet set = localCache.get(slot, bytesKey);
        if (set == null) {
            set = localCacheForWrite.get(slot, bytesKey);
        }
        return set;
    }

    public Set<BytesKey> sadd(byte[] key, byte[] cacheKey, Set<BytesKey> memberSet) {
        int slot = RedisClusterCRC16Utils.getSlot(key);
        BytesKey bytesKey = new BytesKey(cacheKey);
        RedisSet set = localCache.get(slot, bytesKey);
        Set<BytesKey> result = null;
        if (set != null) {
            result = set.sadd(memberSet);
        }
        set = localCacheForWrite.get(slot, bytesKey);
        if (set != null) {
            result = set.sadd(memberSet);
        }
        return result;
    }

    public Set<BytesKey> spop(byte[] key, byte[] cacheKey, int count) {
        int slot = RedisClusterCRC16Utils.getSlot(key);
        BytesKey bytesKey = new BytesKey(cacheKey);
        RedisSet set = localCache.get(slot, bytesKey);
        Set<BytesKey> result = null;
        if (set != null) {
            result = set.spop(count);
        }
        set = localCacheForWrite.get(slot, bytesKey);
        if (set != null) {
            if (result != null) {
                set.srem(result);
            } else {
                result = set.spop(count);
            }
        }
        return result;
    }

    public Set<BytesKey> srem(byte[] key, byte[] cacheKey, Collection<BytesKey> members) {
        int slot = RedisClusterCRC16Utils.getSlot(key);
        BytesKey bytesKey = new BytesKey(cacheKey);
        RedisSet set = localCache.get(slot, bytesKey);
        Set<BytesKey> result = null;
        if (set != null) {
            result = set.srem(members);
        }
        set = localCacheForWrite.get(slot, bytesKey);
        if (set != null) {
            result = set.srem(members);
        }
        return result;
    }

    public void del(byte[] key, byte[] cacheKey) {
        int slot = RedisClusterCRC16Utils.getSlot(key);
        BytesKey bytesKey = new BytesKey(cacheKey);
        localCache.remove(slot, bytesKey);
        localCacheForWrite.remove(slot, bytesKey);
    }

    public void clear() {
        localCache.clear();
        localCacheForWrite.clear();
    }

    public long estimateSize() {
        long estimateSize = 0;
        estimateSize += localCache.estimateSize();
        estimateSize += localCacheForWrite.estimateSize();
        estimateSize += hotKeyCalculator.estimateSize();
        return estimateSize;
    }
}
