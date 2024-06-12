package com.netease.nim.camellia.redis.proxy.upstream.kv.cache;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.netease.nim.camellia.redis.proxy.util.RedisClusterCRC16Utils;
import com.netease.nim.camellia.tools.utils.BytesKey;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Created by caojiajun on 2024/6/12
 */
public class SlotLRUCache<V> {

    private final ConcurrentHashMap<Integer, ConcurrentLinkedHashMap<BytesKey, V>> map;

    public SlotLRUCache(int capacity) {
        map = new ConcurrentHashMap<>();
        for (int i=0; i< RedisClusterCRC16Utils.SLOT_SIZE; i++) {
            ConcurrentLinkedHashMap<BytesKey, V> subMap = new ConcurrentLinkedHashMap.Builder<BytesKey, V>()
                    .initialCapacity(capacity)
                    .maximumWeightedCapacity(capacity)
                    .build();
            map.put(i, subMap);
        }
    }

    public V get(int slot, BytesKey cacheKey) {
        ConcurrentLinkedHashMap<BytesKey, V> subMap = map.get(slot);
        return subMap.get(cacheKey);
    }


    public void put(int slot, BytesKey cacheKey, V value) {
        ConcurrentLinkedHashMap<BytesKey, V> subMap = map.get(slot);
        subMap.put(cacheKey, value);
    }

    public void remove(int slot, BytesKey cacheKey) {
        ConcurrentLinkedHashMap<BytesKey, V> subMap = map.get(slot);
        subMap.remove(cacheKey);
    }

    public void clear() {
        for (Map.Entry<Integer, ConcurrentLinkedHashMap<BytesKey, V>> entry : map.entrySet()) {
            entry.getValue().clear();
        }
    }

    public void clear(int slot) {
        ConcurrentLinkedHashMap<BytesKey, V> subMap = map.get(slot);
        subMap.clear();
    }

    public void setCapacity(int capacity) {
        for (Map.Entry<Integer, ConcurrentLinkedHashMap<BytesKey, V>> entry : map.entrySet()) {
            entry.getValue().setCapacity(capacity);
        }
    }
}
