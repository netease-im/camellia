package com.netease.nim.camellia.redis.proxy.upstream.kv.cache;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.tools.utils.BytesKey;
import com.netease.nim.camellia.tools.utils.MathUtil;


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
            subMap.setCapacity(capacity);
        }
    }
}
