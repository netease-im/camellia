package com.netease.nim.camellia.redis.proxy.upstream.kv.cache;

import com.netease.nim.camellia.redis.proxy.util.Utils;
import com.netease.nim.camellia.tools.utils.BytesKey;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by caojiajun on 2024/6/5
 */
public class RedisHash implements EstimateSizeValue {
    private final ConcurrentHashMap<BytesKey, byte[]> map;
    private long estimateSize = 0;

    public RedisHash(ConcurrentHashMap<BytesKey, byte[]> map) {
        this.map = map;
        for (Map.Entry<BytesKey, byte[]> entry : map.entrySet()) {
            estimateSize += entry.getKey().getKey().length;
            estimateSize += entry.getValue().length;
        }
    }

    public RedisHash duplicate() {
        return new RedisHash(new ConcurrentHashMap<>(map));
    }

    public Map<BytesKey, byte[]> hset(Map<BytesKey, byte[]> fieldMap) {
        Map<BytesKey, byte[]> existsMap = new HashMap<>();
        for (Map.Entry<BytesKey, byte[]> entry : fieldMap.entrySet()) {
            byte[] put = map.put(entry.getKey(), entry.getValue());
            estimateSize += entry.getValue().length;
            if (put != null) {
                existsMap.put(entry.getKey(), put);
                estimateSize -= put.length;
            } else {
                estimateSize += entry.getKey().getKey().length;
            }
        }
        return existsMap;
    }

    public byte[] hset(BytesKey field, byte[] value) {
        byte[] oldValue = map.put(field, value);
        estimateSize += value.length;
        if (oldValue != null) {
            estimateSize -= oldValue.length;
        } else {
            estimateSize += field.getKey().length;
        }
        return oldValue;
    }

    public Map<BytesKey, byte[]> hdel(Collection<BytesKey> fields) {
        Map<BytesKey, byte[]> deleteMap = new HashMap<>();
        for (BytesKey field : fields) {
            byte[] remove = map.remove(field);
            if (remove != null) {
                deleteMap.put(field, remove);
                estimateSize -= field.getKey().length;
                estimateSize -= remove.length;
            }
        }
        return deleteMap;
    }

    public byte[] hget(BytesKey key) {
        return map.get(key);
    }

    public int hstrlen(BytesKey key) {
        byte[] bytes = map.get(key);
        if (bytes == null) {
            return 0;
        }
        return Utils.bytesToString(bytes).length();
    }

    public int hexists(BytesKey key) {
        byte[] bytes = map.get(key);
        if (bytes == null) {
            return 0;
        }
        return 1;
    }

    public int hlen() {
        return map.size();
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public Map<BytesKey, byte[]> hgetAll() {
        return map;
    }

    @Override
    public long estimateSize() {
        return map.size() * 8L + (estimateSize < 0 ? 0 : estimateSize);
    }
}
