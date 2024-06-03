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
import java.util.HashMap;
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
            logger.info("zset lru cache build, capacity = {}", capacity);
        }
        this.capacity = capacity;
    }

    public boolean isHotKey(byte[] key) {
        return hotKeyCalculator.isHotKey(key);
    }

    public void zaddAll(byte[] cacheKey, Map<BytesKey, Double> map) {
        ZSet zSet = new ZSet(new HashMap<>(map));
        localCache.put(new BytesKey(cacheKey), zSet);
    }

    public void putZSet(byte[] cacheKey, ZSet zSet) {
        localCache.put(new BytesKey(cacheKey), zSet);
    }

    public Map<BytesKey, Double> zadd(byte[] cacheKey, Map<BytesKey, Double> map) {
        ZSet zSet = localCache.get(new BytesKey(cacheKey));
        if (zSet == null) {
            return null;
        }
        return zSet.zadd(map);
    }

    public List<ZSetTuple> zrange(byte[] cacheKey, int start, int stop) {
        ZSet zSet = localCache.get(new BytesKey(cacheKey));
        if (zSet == null) {
            return null;
        }
        return zSet.zrange(start, stop);
    }

    public List<ZSetTuple> zrevrange(byte[] cacheKey, int start, int stop) {
        ZSet zSet = localCache.get(new BytesKey(cacheKey));
        if (zSet == null) {
            return null;
        }
        return zSet.zrevrange(start, stop);
    }

    public List<ZSetTuple> zrangebyscore(byte[] cacheKey, ZSetScore minScore, ZSetScore maxScore, ZSetLimit limit) {
        ZSet zSet = localCache.get(new BytesKey(cacheKey));
        if (zSet == null) {
            return null;
        }
        return zSet.zrangebyscore(minScore, maxScore, limit);
    }

    public List<ZSetTuple> zrevrangeByScore(byte[] cacheKey, ZSetScore minScore, ZSetScore maxScore, ZSetLimit limit) {
        ZSet zSet = localCache.get(new BytesKey(cacheKey));
        if (zSet == null) {
            return null;
        }
        return zSet.zrevrangeByScore(minScore, maxScore, limit);
    }

    public List<ZSetTuple> zrangeByLex(byte[] cacheKey, ZSetLex minLex, ZSetLex maxLex, ZSetLimit limit) {
        ZSet zSet = localCache.get(new BytesKey(cacheKey));
        if (zSet == null) {
            return null;
        }
        return zSet.zrangeByLex(minLex, maxLex, limit);
    }

    public List<ZSetTuple> zrevrangeByLex(byte[] cacheKey, ZSetLex minLex, ZSetLex maxLex, ZSetLimit limit) {
        ZSet zSet = localCache.get(new BytesKey(cacheKey));
        if (zSet == null) {
            return null;
        }
        return zSet.zrevrangeByLex(minLex, maxLex, limit);
    }

    public Double zscore(byte[] cacheKey, BytesKey member) {
        ZSet zSet = localCache.get(new BytesKey(cacheKey));
        if (zSet == null) {
            return null;
        }
        return zSet.zscore(member);
    }

    public int zcard(byte[] cacheKey) {
        ZSet zSet = localCache.get(new BytesKey(cacheKey));
        if (zSet == null) {
            return -1;
        }
        return zSet.zcard();
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
