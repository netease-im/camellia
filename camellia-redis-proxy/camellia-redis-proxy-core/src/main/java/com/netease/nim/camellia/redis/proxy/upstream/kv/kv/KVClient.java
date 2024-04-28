package com.netease.nim.camellia.redis.proxy.upstream.kv.kv;

import java.util.List;

/**
 * Created by caojiajun on 2024/4/7
 */
public interface KVClient {

    /**
     * check support ttl for put
     * @return true/false
     */
    boolean supportTTL();

    /**
     * put with ttl
     * @param key key
     * @param value value
     * @param ttl ttl
     */
    void put(byte[] key, byte[] value, long ttl);

    /**
     * put without ttl
     * @param key key
     * @param value value
     */
    void put(byte[] key, byte[] value);

    /**
     * batch put without ttl
     * @param list k-v list
     */
    void batchPut(List<KeyValue> list);

    /**
     * get
     * @param key key
     * @return k-v
     */
    KeyValue get(byte[] key);

    /**
     * check exists key
     * @param key key
     * @return true/false
     */
    boolean exists(byte[] key);

    /**
     * batch exists keys
     * @param keys keys
     * @return true/false array
     */
    boolean[] exists(byte[]... keys);

    /**
     * batch get keys
     * @param keys keys
     * @return k-v list
     */
    List<KeyValue> batchGet(byte[]... keys);

    /**
     * delete
     * @param key key
     */
    void delete(byte[] key);

    /**
     * batch delete
     * @param keys keys
     */
    void batchDelete(byte[]...keys);

    /**
     * check support cas of check and delete
     * @return true/false
     */
    boolean supportCheckAndDelete();

    /**
     * cas of check and delete
     * @param key key
     * @param value old-value
     */
    void checkAndDelete(byte[] key, byte[] value);

    /**
     * scan k-v list with prefix
     * @param startKey start key
     * @param prefix prefix
     * @param limit limit
     * @param sort sort
     * @param includeStartKey include start key
     * @return k-v list
     */
    List<KeyValue> scan(byte[] startKey, byte[] prefix, int limit, Sort sort, boolean includeStartKey);

    /**
     * count key count with key prefix
     * @param startKey start key
     * @param prefix prefix
     * @param includeStartKey include start key
     * @return count
     */
    long count(byte[] startKey, byte[] prefix, boolean includeStartKey);

    /**
     * scan k-v list with start and end
     * @param startKey start key
     * @param endKey end key
     * @param limit limit
     * @param sort sort
     * @param includeStartKey include start key
     * @param includeEndKey include end key
     * @return k-v list
     */
    List<KeyValue> scan(byte[] startKey, byte[] endKey, int limit, Sort sort, boolean includeStartKey, boolean includeEndKey);

    /**
     * count key count with start and end
     * @param startKey start key
     * @param endKey end key
     * @param includeStartKey include start key
     * @param includeEndKey include end key
     * @return count
     */
    long count(byte[] startKey, byte[] endKey, boolean includeStartKey, boolean includeEndKey);
}
