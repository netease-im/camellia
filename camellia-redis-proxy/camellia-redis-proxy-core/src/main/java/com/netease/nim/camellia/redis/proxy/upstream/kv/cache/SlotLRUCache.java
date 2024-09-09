package com.netease.nim.camellia.redis.proxy.upstream.kv.cache;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.tools.utils.BytesKey;
import com.netease.nim.camellia.tools.utils.MathUtil;

import java.util.Map;


/**
 * Created by caojiajun on 2024/6/12
 */
public class SlotLRUCache<V> {

    private final ConcurrentLinkedHashMap<SlotCacheKey, V>[] array;
    private final int segmentSize;
    private final boolean is2Power;

    public SlotLRUCache(int capacity) {
        segmentSize = ProxyDynamicConf.getInt("kv.lru.cache.segment.size", 16);
        is2Power = MathUtil.is2Power(segmentSize);
        array = new ConcurrentLinkedHashMap[segmentSize];
        for (int i = 0; i< segmentSize; i++) {
            ConcurrentLinkedHashMap<SlotCacheKey, V> subMap = new ConcurrentLinkedHashMap.Builder<SlotCacheKey, V>()
                    .initialCapacity(capacity / segmentSize)
                    .maximumWeightedCapacity(capacity / segmentSize)
                    .build();
            array[i] = subMap;
        }
    }

    public V get(SlotCacheKey cacheKey) {
        int index = MathUtil.mod(is2Power, cacheKey.getSlot(), segmentSize);
        ConcurrentLinkedHashMap<SlotCacheKey, V> subMap = array[index];
        return subMap.get(cacheKey);
    }


    public void put(SlotCacheKey cacheKey, V value) {
        int index = MathUtil.mod(is2Power, cacheKey.getSlot(), segmentSize);
        ConcurrentLinkedHashMap<SlotCacheKey, V> subMap = array[index];
        subMap.put(cacheKey, value);
    }

    public void remove(SlotCacheKey cacheKey) {
        int index = MathUtil.mod(is2Power, cacheKey.getSlot(), segmentSize);
        ConcurrentLinkedHashMap<SlotCacheKey, V> subMap = array[index];
        subMap.remove(cacheKey);
    }

    public void clear() {
        for (ConcurrentLinkedHashMap<SlotCacheKey, V> subMap : array) {
            subMap.clear();
        }
    }

    public void clear(int slot) {
        int index = MathUtil.mod(is2Power, slot, segmentSize);
        ConcurrentLinkedHashMap<SlotCacheKey, V> subMap = array[index];
        subMap.entrySet().removeIf(entry -> entry.getKey().getSlot() == slot);
    }

    public void setCapacity(int capacity) {
        for (ConcurrentLinkedHashMap<SlotCacheKey, V> subMap : array) {
            subMap.setCapacity(capacity / segmentSize);
        }
    }

    public long size() {
        long estimateSize = 0;
        for (ConcurrentLinkedHashMap<SlotCacheKey, V> map : array) {
            estimateSize += map.size();
        }
        return estimateSize;
    }

    public long estimateSize() {
        long estimateSize = 0;
        for (ConcurrentLinkedHashMap<SlotCacheKey, V> map : array) {
            estimateSize += map.size() * 12L;
            for (Map.Entry<SlotCacheKey, V> entry : map.entrySet()) {
                estimateSize += entry.getKey().getKey().length;
                V value = entry.getValue();
                if (value instanceof EstimateSizeValue) {
                    long size = ((EstimateSizeValue) value).estimateSize();
                    if (size > 0) {
                        estimateSize += size;
                    }
                } else if (value instanceof BytesKey) {
                    estimateSize += ((BytesKey) value).getKey().length;
                } else if (value instanceof Boolean) {
                    estimateSize += 4;
                } else if (value instanceof Integer) {
                    estimateSize += 4;
                } else if (value instanceof Long) {
                    estimateSize += 8;
                } else if (value instanceof Double) {
                    estimateSize += 8;
                } else {
                    estimateSize += 4;
                }
            }
        }
        return estimateSize;
    }
}
