package com.netease.nim.camellia.redis.proxy.upstream.kv.kv;

import java.util.List;

/**
 * Created by caojiajun on 2024/4/7
 */
public interface KVClient {

    boolean supportTTL();

    void put(byte[] key, byte[] value, long ttl);

    void put(byte[] key, byte[] value);

    void batchPut(List<KeyValue> list);

    KeyValue get(byte[] key);

    boolean exists(byte[] key);

    boolean[] exists(byte[]... keys);

    List<KeyValue> batchGet(byte[]... keys);

    void delete(byte[] key);

    void batchDelete(byte[]...keys);

    boolean supportCheckAndDelete();

    void checkAndDelete(byte[] key, byte[] value);

    List<KeyValue> scan(byte[] startKey, byte[] prefix, int limit, Sort sort, boolean includeStartKey);

    long count(byte[] startKey, byte[] prefix, boolean includeStartKey);

    List<KeyValue> scan(byte[] startKey, byte[] endKey, int limit, Sort sort, boolean includeStartKey, boolean includeEndKey);

    long count(byte[] startKey, byte[] endKey, boolean includeStartKey, boolean includeEndKey);
}
