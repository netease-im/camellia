package com.netease.nim.camellia.redis.proxy.upstream.kv.cache;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.redis.proxy.cluster.ClusterModeStatus;
import com.netease.nim.camellia.redis.proxy.cluster.ProxyClusterSlotMapUtils;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.upstream.kv.conf.RedisKvConf;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyType;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import com.netease.nim.camellia.tools.utils.BytesKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Created by caojiajun on 2024/5/21
 */
public class SetLRUCache {

    private static final Logger logger = LoggerFactory.getLogger(SetLRUCache.class);

    private final String namespace;
    private final HotKeyCalculator hotKeyCalculator;
    private final SlotLRUCache<RedisSet> localCache = new SlotLRUCache<>(10000);
    private final SlotLRUCache<RedisSet> localCacheForWrite = new SlotLRUCache<>(10000);

    private int capacity;

    public SetLRUCache(String namespace) {
        this.namespace = namespace;
        this.hotKeyCalculator = new HotKeyCalculator(namespace, KeyType.set);
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
        int capacity = RedisKvConf.getInt(namespace, "kv.set.lru.cache.capacity", -1);
        if (capacity > 0) {
            if (this.capacity != capacity) {
                this.localCache.setCapacity(capacity);
                this.localCacheForWrite.setCapacity(capacity);
                logger.info("set lru cache build, namespace = {}, capacity = {}", namespace, capacity);
            }
            this.capacity = capacity;
        } else {
            CacheCapacityCalculator.update(localCache, namespace, "kv.set.lru.read.cache.size");
            CacheCapacityCalculator.update(localCacheForWrite, namespace, "kv.set.lru.write.cache.size");
        }
    }

    public boolean isHotKey(byte[] key, RedisCommand redisCommand) {
        return hotKeyCalculator.isHotKey(key, redisCommand);
    }

    public void putAllForRead(int slot, byte[] cacheKey, RedisSet set) {
        SlotCacheKey slotCacheKey = new SlotCacheKey(slot, cacheKey);
        localCache.put(slotCacheKey, set);
    }

    public void putAllForWrite(int slot, byte[] cacheKey, RedisSet set) {
        SlotCacheKey slotCacheKey = new SlotCacheKey(slot, cacheKey);
        localCacheForWrite.put(slotCacheKey, set);
    }

    public RedisSet getForRead(int slot, byte[] cacheKey) {
        SlotCacheKey slotCacheKey = new SlotCacheKey(slot, cacheKey);
        RedisSet set = localCache.get(slotCacheKey);
        if (set == null) {
            set = localCacheForWrite.get(slotCacheKey);
            if (set != null) {
                localCache.put(slotCacheKey, set);
                localCacheForWrite.remove(slotCacheKey);
            }
        }
        return set;
    }

    public RedisSet getForWrite(int slot, byte[] cacheKey) {
        SlotCacheKey slotCacheKey = new SlotCacheKey(slot, cacheKey);
        RedisSet set = localCache.get(slotCacheKey);
        if (set == null) {
            set = localCacheForWrite.get(slotCacheKey);
        }
        return set;
    }

    public Set<BytesKey> sadd(int slot, byte[] cacheKey, Set<BytesKey> memberSet) {
        SlotCacheKey slotCacheKey = new SlotCacheKey(slot, cacheKey);
        RedisSet set = localCache.get(slotCacheKey);
        Set<BytesKey> result = null;
        if (set != null) {
            result = set.sadd(memberSet);
        }
        set = localCacheForWrite.get(slotCacheKey);
        if (set != null) {
            result = set.sadd(memberSet);
        }
        return result;
    }

    public Set<BytesKey> spop(int slot, byte[] cacheKey, int count) {
        SlotCacheKey slotCacheKey = new SlotCacheKey(slot, cacheKey);
        RedisSet set = localCache.get(slotCacheKey);
        Set<BytesKey> result = null;
        if (set != null) {
            result = set.spop(count);
        }
        set = localCacheForWrite.get(slotCacheKey);
        if (set != null) {
            if (result != null) {
                set.srem(result);
            } else {
                result = set.spop(count);
            }
        }
        return result;
    }

    public Set<BytesKey> srem(int slot, byte[] cacheKey, Collection<BytesKey> members) {
        SlotCacheKey slotCacheKey = new SlotCacheKey(slot, cacheKey);
        RedisSet set = localCache.get(slotCacheKey);
        Set<BytesKey> result = null;
        if (set != null) {
            result = set.srem(members);
        }
        set = localCacheForWrite.get(slotCacheKey);
        if (set != null) {
            result = set.srem(members);
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
            long estimateSize = hotKeyCalculator.estimateSize();
            total += estimateSize;
            json.put("hot.key.calculator.estimate.size", Utils.humanReadableByteCountBin(estimateSize));
            json.put("hot.key.calculator.key.count", hotKeyCalculator.size());
            json.put("hot.key.calculator.key.capacity", hotKeyCalculator.getCapacity());
        }
        {
            json.put("total.estimate.size", Utils.humanReadableByteCountBin(total));
        }
        return json;
    }
}
