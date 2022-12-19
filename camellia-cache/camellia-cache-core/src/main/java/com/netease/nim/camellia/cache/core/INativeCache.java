package com.netease.nim.camellia.cache.core;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface INativeCache {

    void put(String key, Object value);

    void put(String key, Object value, long expireMillis);

    void multiPut(Map<String, Object> kvs);

    void multiPut(Map<String, Object> kvs, long expireMillis);

    Object get(String key);

    List<Object> multiGet(Collection<String> keys);

    void delete(String key);

    void multiDelete(Collection<String> keys);

    boolean acquireLock(String key, long expireMillis);

    void releaseLock(String key);
}
