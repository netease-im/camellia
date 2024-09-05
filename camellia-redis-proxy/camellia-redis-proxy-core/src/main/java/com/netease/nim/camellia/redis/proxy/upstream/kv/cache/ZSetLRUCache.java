package com.netease.nim.camellia.redis.proxy.upstream.kv.cache;

import com.netease.nim.camellia.redis.proxy.cluster.ClusterModeStatus;
import com.netease.nim.camellia.redis.proxy.cluster.ProxyClusterSlotMapUtils;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.zset.utils.ZSetLex;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.zset.utils.ZSetScore;
import com.netease.nim.camellia.redis.proxy.upstream.kv.conf.RedisKvConf;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyType;
import com.netease.nim.camellia.tools.utils.BytesKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Created by caojiajun on 2024/5/31
 */
public class ZSetLRUCache {

    private static final Logger logger = LoggerFactory.getLogger(ZSetLRUCache.class);

    private final String namespace;
    private final HotKeyCalculator hotKeyCalculator;

    private int capacity;
    private SlotLRUCache<RedisZSet> localCache;
    private SlotLRUCache<RedisZSet> localCacheForWrite;

    public ZSetLRUCache(String namespace) {
        this.namespace = namespace;
        this.hotKeyCalculator = new HotKeyCalculator(namespace, KeyType.zset);
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
        int capacity = RedisKvConf.getInt(namespace, "kv.zset.lru.cache.capacity", 10_0000);
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
            logger.info("zset lru cache build, namespace = {}, capacity = {}", namespace, capacity);
        }
        this.capacity = capacity;
    }

    public boolean isHotKey(byte[] key) {
        return hotKeyCalculator.isHotKey(key);
    }

    public void putZSetForWrite(int slot, byte[] cacheKey, RedisZSet zSet) {
        localCacheForWrite.put(slot, new BytesKey(cacheKey), zSet);
    }

    public void putZSetForRead(int slot, byte[] cacheKey, RedisZSet zSet) {
        localCache.put(slot, new BytesKey(cacheKey), zSet);
    }

    public RedisZSet getForRead(int slot, byte[] cacheKey) {
        BytesKey bytesKey = new BytesKey(cacheKey);
        RedisZSet zSet = localCache.get(slot, bytesKey);
        if (zSet == null) {
            zSet = localCacheForWrite.get(slot, bytesKey);
            if (zSet != null) {
                localCache.put(slot, bytesKey, zSet);
                localCacheForWrite.remove(slot, bytesKey);
            }
        }
        return zSet;
    }

    public RedisZSet getForWrite(int slot, byte[] cacheKey) {
        BytesKey bytesKey = new BytesKey(cacheKey);
        RedisZSet zSet = localCache.get(slot, bytesKey);
        if (zSet == null) {
            zSet = localCacheForWrite.get(slot, bytesKey);
        }
        return zSet;
    }

    public Map<BytesKey, Double> zadd(int slot, byte[] cacheKey, Map<BytesKey, Double> map) {
        RedisZSet zSet = localCache.get(slot, new BytesKey(cacheKey));
        Map<BytesKey, Double> result = null;
        if (zSet != null) {
            result = zSet.zadd(map);
        }
        zSet = localCacheForWrite.get(slot, new BytesKey(cacheKey));
        if (zSet != null) {
            result = zSet.zadd(map);
        }
        return result;
    }

    public Map<BytesKey, Double> zrem(int slot, byte[] cacheKey, Collection<BytesKey> members) {
        Map<BytesKey, Double> result = null;
        BytesKey bytesKey = new BytesKey(cacheKey);
        RedisZSet zSet = localCache.get(slot, bytesKey);
        if (zSet != null) {
            result = zSet.zrem(members);
        }
        zSet = localCacheForWrite.get(slot, bytesKey);
        if (zSet != null) {
            result = zSet.zrem(members);
        }
        return result;
    }

    public Map<BytesKey, Double> zremrangeByRank(int slot, byte[] cacheKey, int start, int stop) {
        BytesKey bytesKey = new BytesKey(cacheKey);
        Map<BytesKey, Double> result = null;
        RedisZSet zSet = localCache.get(slot, bytesKey);
        if (zSet != null) {
            result = zSet.zremrangeByRank(start, stop);
        }
        zSet = localCacheForWrite.get(slot, bytesKey);
        if (zSet != null) {
            result = zSet.zremrangeByRank(start, stop);
        }
        return result;
    }

    public Map<BytesKey, Double> zremrangeByScore(int slot, byte[] cacheKey, ZSetScore minScore, ZSetScore maxScore) {
        BytesKey bytesKey = new BytesKey(cacheKey);
        Map<BytesKey, Double> result = null;

        RedisZSet zSet = localCache.get(slot, bytesKey);
        if (zSet != null) {
            result = zSet.zremrangeByScore(minScore, maxScore);
        }
        zSet = localCacheForWrite.get(slot, bytesKey);
        if (zSet != null) {
            result = zSet.zremrangeByScore(minScore, maxScore);
        }
        return result;
    }

    public Map<BytesKey, Double> zremrangeByLex(int slot, byte[] cacheKey, ZSetLex minLex, ZSetLex maxLex) {
        BytesKey bytesKey = new BytesKey(cacheKey);
        Map<BytesKey, Double> result = null;

        RedisZSet zSet = localCache.get(slot, bytesKey);
        if (zSet != null) {
            result = zSet.zremrangeByLex(minLex, maxLex);
        }
        zSet = localCacheForWrite.get(slot, bytesKey);
        if (zSet != null) {
            result = zSet.zremrangeByLex(minLex, maxLex);
        }
        return result;
    }

    public void del(int slot, byte[] cacheKey) {
        BytesKey bytesKey = new BytesKey(cacheKey);
        localCache.remove(slot, bytesKey);
        localCacheForWrite.remove(slot, bytesKey);
    }

    public void clear() {
        localCache.clear();
    }

    public long estimateSize() {
        long estimateSize = 0;
        estimateSize += localCache.estimateSize();
        estimateSize += localCacheForWrite.estimateSize();
        estimateSize += hotKeyCalculator.estimateSize();
        return estimateSize;
    }
}
