package com.netease.nim.camellia.redis.proxy.upstream.kv.cache;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.netease.nim.camellia.redis.proxy.cluster.ClusterModeStatus;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.zset.*;
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
    private ConcurrentLinkedHashMap<BytesKey, ZSet> localCache;
    private ConcurrentLinkedHashMap<BytesKey, ZSet> localCacheForWrite;

    public ZSetLRUCache(String namespace) {
        this.namespace = namespace;
        this.hotKeyCalculator = new HotKeyCalculator(namespace, KeyType.zset);
        rebuild();
        ProxyDynamicConf.registerCallback(this::rebuild);
        ClusterModeStatus.registerClusterModeSlotRefreshCallback(localCache::clear);
    }

    private void rebuild() {
        int capacity = RedisKvConf.getInt(namespace, "kv.zset.lru.cache.capacity", 500000);
        if (this.capacity != capacity) {
            if (this.localCache == null) {
                this.localCache = new ConcurrentLinkedHashMap.Builder<BytesKey, ZSet>()
                        .initialCapacity(capacity)
                        .maximumWeightedCapacity(capacity)
                        .build();
            } else {
                this.localCache.setCapacity(capacity);
            }
            if (this.localCacheForWrite == null) {
                this.localCacheForWrite = new ConcurrentLinkedHashMap.Builder<BytesKey, ZSet>()
                        .initialCapacity(capacity)
                        .maximumWeightedCapacity(capacity)
                        .build();
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

    public void putZSetForWrite(byte[] cacheKey, ZSet zSet) {
        localCacheForWrite.put(new BytesKey(cacheKey), zSet);
    }

    public void putZSetForRead(byte[] cacheKey, ZSet zSet) {
        localCache.put(new BytesKey(cacheKey), zSet);
    }

    public ZSet getForRead(byte[] cacheKey) {
        BytesKey bytesKey = new BytesKey(cacheKey);
        ZSet zSet = localCache.get(bytesKey);
        if (zSet == null) {
            zSet = localCacheForWrite.get(bytesKey);
            if (zSet != null) {
                localCache.put(bytesKey, zSet);
                localCacheForWrite.remove(bytesKey);
            }
        }
        return zSet;
    }

    public ZSet getForWrite(byte[] cacheKey) {
        BytesKey bytesKey = new BytesKey(cacheKey);
        ZSet zSet = localCache.get(bytesKey);
        if (zSet == null) {
            zSet = localCacheForWrite.get(bytesKey);
        }
        return zSet;
    }

    public Map<BytesKey, Double> zadd(byte[] cacheKey, Map<BytesKey, Double> map) {
        ZSet zSet1 = localCache.get(new BytesKey(cacheKey));
        Map<BytesKey, Double> existsMap1 = null;
        if (zSet1 != null) {
            existsMap1 = zSet1.zadd(map);
        }
        ZSet zSet2 = localCacheForWrite.get(new BytesKey(cacheKey));
        Map<BytesKey, Double> existsMap2 = null;
        if (zSet2 != null) {
            existsMap2 = zSet2.zadd(map);
        }
        if (existsMap1 != null) {
            return existsMap1;
        }
        return existsMap2;
    }

    public Map<BytesKey, Double> zrem(byte[] cacheKey, Collection<BytesKey> members) {
        ZSet zSet = localCache.get(new BytesKey(cacheKey));
        if (zSet == null) {
            return null;
        }
        return zSet.zrem(members);
    }

    public Map<BytesKey, Double> zremrangeByRank(byte[] cacheKey, int start, int stop) {
        ZSet zSet = localCache.get(new BytesKey(cacheKey));
        if (zSet == null) {
            return null;
        }
        return zSet.zremrangeByRank(start, stop);
    }

    public Map<BytesKey, Double> zremrangeByScore(byte[] cacheKey, ZSetScore minScore, ZSetScore maxScore) {
        ZSet zSet = localCache.get(new BytesKey(cacheKey));
        if (zSet == null) {
            return null;
        }
        return zSet.zremrangeByScore(minScore, maxScore);
    }

    public Map<BytesKey, Double> zremrangeByLex(byte[] cacheKey, ZSetLex minLex, ZSetLex maxLex) {
        ZSet zSet = localCache.get(new BytesKey(cacheKey));
        if (zSet == null) {
            return null;
        }
        return zSet.zremrangeByLex(minLex, maxLex);
    }

    public void clear() {
        localCache.clear();
    }
}
