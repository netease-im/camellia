package com.netease.nim.camellia.redis.proxy.upstream.kv.cache;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.netease.nim.camellia.redis.proxy.cluster.ClusterModeStatus;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.zset.ZSetLex;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.zset.ZSetLimit;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.zset.ZSetScore;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.zset.ZSetTuple;
import com.netease.nim.camellia.redis.proxy.upstream.kv.conf.RedisKvConf;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import com.netease.nim.camellia.tools.utils.BytesKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by caojiajun on 2024/5/31
 */
public class ZSetLRUCache {

    private static final Logger logger = LoggerFactory.getLogger(ZSetLRUCache.class);

    private final String namespace;
    private int capacity;
    private ConcurrentLinkedHashMap<BytesKey, ZSet> localCache;

    public ZSetLRUCache(String namespace) {
        this.namespace = namespace;
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

    public void zaddAll(byte[] cacheKey, Map<BytesKey, byte[]> map) {
        Map<BytesKey, Double> map1 = new HashMap<>();
        for (Map.Entry<BytesKey, byte[]> entry : map.entrySet()) {
            map1.put(entry.getKey(), Double.parseDouble(Utils.bytesToString(entry.getValue())));
        }
        ZSet zSet = new ZSet(map1);
        localCache.put(new BytesKey(cacheKey), zSet);
    }

    public void zaddAll(byte[] cacheKey, List<ZSetTuple> list) {
        Map<BytesKey, Double> map1 = new HashMap<>();
        for (ZSetTuple tuple : list) {
            map1.put(tuple.getMember(), Double.parseDouble(Utils.bytesToString(tuple.getScore().getKey())));
        }
        ZSet zSet = new ZSet(map1);
        localCache.put(new BytesKey(cacheKey), zSet);
    }

    public Map<BytesKey, Double> zadd(byte[] cacheKey, Map<BytesKey, byte[]> map) {
        ZSet zSet = localCache.get(new BytesKey(cacheKey));
        if (zSet == null) {
            return null;
        }
        Map<BytesKey, Double> map1 = new HashMap<>();
        for (Map.Entry<BytesKey, byte[]> entry : map.entrySet()) {
            map1.put(entry.getKey(), Utils.bytesToDouble(entry.getValue()));
        }
        return zSet.zadd(map1);
    }

    public List<ZSet.Member> zrange(byte[] cacheKey, int start, int stop) {
        ZSet zSet = localCache.get(new BytesKey(cacheKey));
        if (zSet == null) {
            return null;
        }
        return zSet.zrange(start, stop);
    }

    public List<ZSet.Member> zrevrange(byte[] cacheKey, int start, int stop) {
        ZSet zSet = localCache.get(new BytesKey(cacheKey));
        if (zSet == null) {
            return null;
        }
        return zSet.zrevrange(start, stop);
    }

    public List<ZSet.Member> zrangebyscore(byte[] cacheKey, ZSetScore minScore, ZSetScore maxScore, ZSetLimit limit) {
        ZSet zSet = localCache.get(new BytesKey(cacheKey));
        if (zSet == null) {
            return null;
        }
        return zSet.zrangebyscore(minScore, maxScore, limit);
    }

    public List<ZSet.Member> zrevrangeByScore(byte[] cacheKey, ZSetScore minScore, ZSetScore maxScore, ZSetLimit limit) {
        ZSet zSet = localCache.get(new BytesKey(cacheKey));
        if (zSet == null) {
            return null;
        }
        return zSet.zrevrangeByScore(minScore, maxScore, limit);
    }

    public List<ZSet.Member> zrangeByLex(byte[] cacheKey, ZSetLex minLex, ZSetLex maxLex, ZSetLimit limit) {
        ZSet zSet = localCache.get(new BytesKey(cacheKey));
        if (zSet == null) {
            return null;
        }
        return zSet.zrangeByLex(minLex, maxLex, limit);
    }

    public List<ZSet.Member> zrevrangeByLex(byte[] cacheKey, ZSetLex minLex, ZSetLex maxLex, ZSetLimit limit) {
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

    public Map<BytesKey, Double> zrem(byte[] cacheKey, List<BytesKey> members) {
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
