package com.netease.nim.camellia.redis.proxy.upstream.kv.cache;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.netease.nim.camellia.redis.proxy.util.RedisClusterCRC16Utils;
import com.netease.nim.camellia.tools.utils.BytesKey;


/**
 * Created by caojiajun on 2024/6/12
 */
public class SlotLRUCache<V> {

    private final ConcurrentLinkedHashMap<BytesKey, V>[] array = new ConcurrentLinkedHashMap[RedisClusterCRC16Utils.SLOT_SIZE];

    public SlotLRUCache(int capacity) {
        for (int i=0; i< RedisClusterCRC16Utils.SLOT_SIZE; i++) {
            ConcurrentLinkedHashMap<BytesKey, V> subMap = new ConcurrentLinkedHashMap.Builder<BytesKey, V>()
                    .initialCapacity(capacity)
                    .maximumWeightedCapacity(capacity)
                    .build();
            array[i] = subMap;
        }
    }

    public V get(int slot, BytesKey cacheKey) {
        ConcurrentLinkedHashMap<BytesKey, V> subMap = array[slot];
        return subMap.get(cacheKey);
    }


    public void put(int slot, BytesKey cacheKey, V value) {
        ConcurrentLinkedHashMap<BytesKey, V> subMap = array[slot];
        subMap.put(cacheKey, value);
    }

    public void remove(int slot, BytesKey cacheKey) {
        ConcurrentLinkedHashMap<BytesKey, V> subMap = array[slot];
        subMap.remove(cacheKey);
    }

    public void clear() {
        for (ConcurrentLinkedHashMap<BytesKey, V> subMap : array) {
            subMap.clear();
        }
    }

    public void clear(int slot) {
        ConcurrentLinkedHashMap<BytesKey, V> subMap = array[slot];
        subMap.clear();
    }

    public void setCapacity(int capacity) {
        for (ConcurrentLinkedHashMap<BytesKey, V> subMap : array) {
            subMap.setCapacity(capacity);
        }
    }
}
