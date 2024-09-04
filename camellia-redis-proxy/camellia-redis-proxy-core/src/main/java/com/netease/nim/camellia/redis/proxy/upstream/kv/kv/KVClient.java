package com.netease.nim.camellia.redis.proxy.upstream.kv.kv;

import java.util.List;

/**
 * Created by caojiajun on 2024/4/7
 */
public interface KVClient {

    void init(String namespace);

    /**
     * check support ttl for put
     * @return true/false
     */
    boolean supportTTL();

    /**
     * put with ttl
     * @param slot key in slot
     * @param key key
     * @param value value
     * @param ttl ttl
     */
    void put(int slot, byte[] key, byte[] value, long ttl);

    /**
     * put without ttl
     * @param slot key in slot
     * @param key key
     * @param value value
     */
    void put(int slot, byte[] key, byte[] value);

    /**
     * batch put without ttl
     * @param slot key in slot
     * @param list k-v list
     */
    void batchPut(int slot, List<KeyValue> list);

    /**
     * get
     * @param slot key in slot
     * @param key key
     * @return k-v
     */
    KeyValue get(int slot, byte[] key);

    /**
     * check exists key
     * @param slot key in slot
     * @param key key
     * @return true/false
     */
    boolean exists(int slot, byte[] key);

    /**
     * batch exists keys
     * @param slot key in slot
     * @param keys keys
     * @return true/false array
     */
    boolean[] exists(int slot, byte[]... keys);

    /**
     * batch get keys
     * @param slot key in slot
     * @param keys keys
     * @return k-v list
     */
    List<KeyValue> batchGet(int slot, byte[]... keys);

    /**
     * delete
     * @param slot key in slot
     * @param key key
     */
    void delete(int slot, byte[] key);

    /**
     * batch delete
     * @param slot key in slot
     * @param keys keys
     */
    void batchDelete(int slot, byte[]...keys);

    /**
     * check support cas of check and delete
     * @return true/false
     */
    boolean supportCheckAndDelete();

    /**
     * cas of check and delete
     * @param slot key in slot
     * @param key key
     * @param value old-value
     */
    void checkAndDelete(int slot, byte[] key, byte[] value);

    /**
     * support reverse scan
     * @return true/false
     */
    boolean supportReverseScan();

    /**
     * scan k-v list with prefix
     * @param slot key in slot
     * @param startKey start key
     * @param prefix prefix
     * @param limit limit
     * @param sort sort
     * @param includeStartKey include start key
     * @return k-v list
     */
    List<KeyValue> scanByPrefix(int slot, byte[] startKey, byte[] prefix, int limit, Sort sort, boolean includeStartKey);

    /**
     * count key count with key prefix
     * @param slot key in slot
     * @param startKey start key
     * @param prefix prefix
     * @param includeStartKey include start key
     * @return count
     */
    long countByPrefix(int slot, byte[] startKey, byte[] prefix, boolean includeStartKey);

    /**
     * scan k-v list with start and end
     * @param slot key in slot
     * @param startKey start key
     * @param endKey end key
     * @param prefix prefix
     * @param limit limit
     * @param sort sort
     * @param includeStartKey include start key
     * @return k-v list
     */
    List<KeyValue> scanByStartEnd(int slot, byte[] startKey, byte[] endKey, byte[] prefix, int limit, Sort sort, boolean includeStartKey);

    /**
     * count key count with start and end
     * @param slot key in slot
     * @param startKey start key
     * @param endKey end key
     * @param prefix prefix
     * @param includeStartKey include start key
     * @return count
     */
    long countByStartEnd(int slot, byte[] startKey, byte[] endKey, byte[] prefix, boolean includeStartKey);
}
