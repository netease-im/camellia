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

    private final ConcurrentLinkedHashMap<BytesKey, V>[] array;
    private final int segmentSize;
    private final boolean is2Power;

    public SlotLRUCache(int capacity) {
        segmentSize = ProxyDynamicConf.getInt("kv.lru.cache.segment.size", 16);
        is2Power = MathUtil.is2Power(segmentSize);
        array = new ConcurrentLinkedHashMap[segmentSize];
        for (int i = 0; i< segmentSize; i++) {
            ConcurrentLinkedHashMap<BytesKey, V> subMap = new ConcurrentLinkedHashMap.Builder<BytesKey, V>()
                    .initialCapacity(capacity / segmentSize)
                    .maximumWeightedCapacity(capacity / segmentSize)
                    .build();
            array[i] = subMap;
        }
    }

    public V get(int slot, BytesKey cacheKey) {
        int index = MathUtil.mod(is2Power, slot, segmentSize);
        ConcurrentLinkedHashMap<BytesKey, V> subMap = array[index];
        return subMap.get(cacheKey);
    }


    public void put(int slot, BytesKey cacheKey, V value) {
        int index = MathUtil.mod(is2Power, slot, segmentSize);
        ConcurrentLinkedHashMap<BytesKey, V> subMap = array[index];
        subMap.put(cacheKey, value);
    }

    public void remove(int slot, BytesKey cacheKey) {
        int index = MathUtil.mod(is2Power, slot, segmentSize);
        ConcurrentLinkedHashMap<BytesKey, V> subMap = array[index];
        subMap.remove(cacheKey);
    }

    public void clear() {
        for (ConcurrentLinkedHashMap<BytesKey, V> subMap : array) {
            subMap.clear();
        }
    }

    public void clear(int slot) {
        int index = MathUtil.mod(is2Power, slot, segmentSize);
        ConcurrentLinkedHashMap<BytesKey, V> subMap = array[index];
        subMap.clear();
    }

    public void setCapacity(int capacity) {
        for (ConcurrentLinkedHashMap<BytesKey, V> subMap : array) {
            subMap.setCapacity(capacity / segmentSize);
        }
    }

    public long size() {
        long estimateSize = 0;
        for (ConcurrentLinkedHashMap<BytesKey, V> map : array) {
            estimateSize += map.size();
        }
        return estimateSize;
    }

    public long estimateSize() {
        long estimateSize = 0;
        for (ConcurrentLinkedHashMap<BytesKey, V> map : array) {
            estimateSize += map.size() * 8L;
            for (Map.Entry<BytesKey, V> entry : map.entrySet()) {
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
