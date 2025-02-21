package com.netease.nim.camellia.redis.proxy.upstream.kv.cache;

import com.netease.nim.camellia.redis.proxy.cluster.ClusterModeStatus;
import com.netease.nim.camellia.redis.proxy.cluster.ProxyClusterSlotMapUtils;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.monitor.KvLRUCacheMonitor;
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
import java.util.concurrent.TimeUnit;

/**
 * Created by caojiajun on 2024/5/31
 */
public class ZSetLRUCache {

    private static final Logger logger = LoggerFactory.getLogger(ZSetLRUCache.class);

    private final String namespace;
    private final HotKeyCalculator hotKeyCalculator;
    private final SlotLRUCache<RedisZSet> localCache = new SlotLRUCache<>(10000);
    private final SlotLRUCache<RedisZSet> localCacheForWrite = new SlotLRUCache<>(10000);

    private int capacity;

    public ZSetLRUCache(String namespace) {
        this.namespace = namespace;
        this.hotKeyCalculator = new HotKeyCalculator(namespace, KeyType.zset);
        CacheCapacityCalculator.scheduleAtFixedRate(this::rebuild, 10, 10, TimeUnit.SECONDS);
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
        int capacity = RedisKvConf.getInt(namespace, "kv.zset.lru.cache.capacity", -1);
        if (capacity > 0) {
            if (this.capacity != capacity) {
                this.localCache.setCapacity(capacity);
                this.localCacheForWrite.setCapacity(capacity);
                logger.info("zset lru cache build, namespace = {}, capacity = {}", namespace, capacity);
            }
            KvLRUCacheMonitor.update(namespace, "kv.zset.lru.read.cache", localCache.getCapacity(), localCache.size(), localCache.estimateSize(), 0);
            KvLRUCacheMonitor.update(namespace, "kv.zset.lru.write.cache", localCacheForWrite.getCapacity(), localCacheForWrite.size(), localCacheForWrite.estimateSize(), 0);
            this.capacity = capacity;
        } else {
            CacheCapacityCalculator.update(localCache, namespace, "kv.zset.lru.read.cache");
            CacheCapacityCalculator.update(localCacheForWrite, namespace, "kv.zset.lru.write.cache");
            this.capacity = capacity;
        }
    }

    public boolean isHotKey(byte[] key, RedisCommand redisCommand) {
        return hotKeyCalculator.isHotKey(key, redisCommand);
    }

    public void putZSetForWrite(int slot, byte[] cacheKey, RedisZSet zSet) {
        SlotCacheKey slotCacheKey = new SlotCacheKey(slot, cacheKey);
        localCacheForWrite.put(slotCacheKey, zSet);
    }

    public void putZSetForRead(int slot, byte[] cacheKey, RedisZSet zSet) {
        SlotCacheKey slotCacheKey = new SlotCacheKey(slot, cacheKey);
        localCache.put(slotCacheKey, zSet);
    }

    public RedisZSet getForRead(int slot, byte[] cacheKey) {
        SlotCacheKey slotCacheKey = new SlotCacheKey(slot, cacheKey);
        RedisZSet zSet = localCache.get(slotCacheKey);
        if (zSet == null) {
            zSet = localCacheForWrite.get(slotCacheKey);
            if (zSet != null) {
                localCache.put(slotCacheKey, zSet);
                localCacheForWrite.remove(slotCacheKey);
            }
        }
        return zSet;
    }

    public RedisZSet getForWrite(int slot, byte[] cacheKey) {
        SlotCacheKey slotCacheKey = new SlotCacheKey(slot, cacheKey);
        RedisZSet zSet = localCache.get(slotCacheKey);
        if (zSet == null) {
            zSet = localCacheForWrite.get(slotCacheKey);
        }
        return zSet;
    }

    public Map<BytesKey, Double> zadd(int slot, byte[] cacheKey, Map<BytesKey, Double> map) {
        SlotCacheKey slotCacheKey = new SlotCacheKey(slot, cacheKey);
        RedisZSet zSet = localCache.get(slotCacheKey);
        Map<BytesKey, Double> result = null;
        if (zSet != null) {
            result = zSet.zadd(map);
        }
        zSet = localCacheForWrite.get(slotCacheKey);
        if (zSet != null) {
            result = zSet.zadd(map);
        }
        return result;
    }

    public Map<BytesKey, Double> zrem(int slot, byte[] cacheKey, Collection<BytesKey> members) {
        SlotCacheKey slotCacheKey = new SlotCacheKey(slot, cacheKey);
        Map<BytesKey, Double> result = null;
        RedisZSet zSet = localCache.get(slotCacheKey);
        if (zSet != null) {
            result = zSet.zrem(members);
        }
        zSet = localCacheForWrite.get(slotCacheKey);
        if (zSet != null) {
            result = zSet.zrem(members);
        }
        return result;
    }

    public Map<BytesKey, Double> zremrangeByRank(int slot, byte[] cacheKey, int start, int stop) {
        SlotCacheKey slotCacheKey = new SlotCacheKey(slot, cacheKey);
        Map<BytesKey, Double> result = null;
        RedisZSet zSet = localCache.get(slotCacheKey);
        if (zSet != null) {
            result = zSet.zremrangeByRank(start, stop);
        }
        zSet = localCacheForWrite.get(slotCacheKey);
        if (zSet != null) {
            result = zSet.zremrangeByRank(start, stop);
        }
        return result;
    }

    public Map<BytesKey, Double> zremrangeByScore(int slot, byte[] cacheKey, ZSetScore minScore, ZSetScore maxScore) {
        SlotCacheKey slotCacheKey = new SlotCacheKey(slot, cacheKey);
        Map<BytesKey, Double> result = null;
        RedisZSet zSet = localCache.get(slotCacheKey);
        if (zSet != null) {
            result = zSet.zremrangeByScore(minScore, maxScore);
        }
        zSet = localCacheForWrite.get(slotCacheKey);
        if (zSet != null) {
            result = zSet.zremrangeByScore(minScore, maxScore);
        }
        return result;
    }

    public Map<BytesKey, Double> zremrangeByLex(int slot, byte[] cacheKey, ZSetLex minLex, ZSetLex maxLex) {
        SlotCacheKey slotCacheKey = new SlotCacheKey(slot, cacheKey);
        Map<BytesKey, Double> result = null;
        RedisZSet zSet = localCache.get(slotCacheKey);
        if (zSet != null) {
            result = zSet.zremrangeByLex(minLex, maxLex);
        }
        zSet = localCacheForWrite.get(slotCacheKey);
        if (zSet != null) {
            result = zSet.zremrangeByLex(minLex, maxLex);
        }
        return result;
    }

    public void del(int slot, byte[] cacheKey) {
        SlotCacheKey slotCacheKey = new SlotCacheKey(slot, cacheKey);
        localCache.remove(slotCacheKey);
        localCacheForWrite.remove(slotCacheKey);
    }

    public void clear() {
        localCache.clear();
    }
}
