package com.netease.nim.camellia.redis.proxy.upstream.kv.kv;

import com.netease.nim.camellia.redis.proxy.monitor.KvStorageMonitor;
import com.netease.nim.camellia.redis.proxy.monitor.ProxyMonitorCollector;

import java.util.Collections;
import java.util.List;

/**
 * Created by caojiajun on 2024/6/13
 */
public class DecoratorKVClient implements KVClient {

    private final String namespace;
    private final KVClient kvClient;
    private final String name;

    public DecoratorKVClient(String namespace, KVClient kvClient) {
        this.namespace = namespace;
        this.kvClient = kvClient;
        this.name = kvClient.getClass().getSimpleName();
    }

    @Override
    public void init(String namespace) {
    }

    @Override
    public boolean supportTTL() {
        return kvClient.supportTTL();
    }

    @Override
    public void put(byte[] key, byte[] value, long ttl) {
        if (ProxyMonitorCollector.isMonitorEnable()) {
            long start = System.nanoTime();
            try {
                kvClient.put(key, value, ttl);
            } finally {
                long spend = System.nanoTime() - start;
                KvStorageMonitor.update(namespace, name, "putWithTTL", spend);
            }
        } else {
            kvClient.put(key, value, ttl);
        }
    }

    @Override
    public void put(byte[] key, byte[] value) {
        if (ProxyMonitorCollector.isMonitorEnable()) {
            long start = System.nanoTime();
            try {
                kvClient.put(key, value);
            } finally {
                long spend = System.nanoTime() - start;
                KvStorageMonitor.update(namespace, name, "put", spend);
            }
        } else {
            kvClient.put(key, value);
        }
    }

    @Override
    public void batchPut(List<KeyValue> list) {
        if (list.isEmpty()) {
            return;
        }
        if (ProxyMonitorCollector.isMonitorEnable()) {
            long start = System.nanoTime();
            String method = "batchPut_" + list.size();
            try {
                kvClient.batchPut(list);
            } finally {
                long spend = System.nanoTime() - start;
                KvStorageMonitor.update(namespace, name, method, spend);
            }
        } else {
            kvClient.batchPut(list);
        }
    }

    @Override
    public KeyValue get(byte[] key) {
        if (ProxyMonitorCollector.isMonitorEnable()) {
            long start = System.nanoTime();
            try {
                return kvClient.get(key);
            } finally {
                long spend = System.nanoTime() - start;
                KvStorageMonitor.update(namespace, name, "get", spend);
            }
        } else {
            return kvClient.get(key);
        }
    }

    @Override
    public boolean exists(byte[] key) {
        if (ProxyMonitorCollector.isMonitorEnable()) {
            long start = System.nanoTime();
            try {
                return kvClient.exists(key);
            } finally {
                long spend = System.nanoTime() - start;
                KvStorageMonitor.update(namespace, name, "exists", spend);
            }
        } else {
            return kvClient.exists(key);
        }
    }

    @Override
    public boolean[] exists(byte[]... keys) {
        if (keys.length == 0) {
            return new boolean[0];
        }
        if (ProxyMonitorCollector.isMonitorEnable()) {
            long start = System.nanoTime();
            String method = "exists_" + keys.length;
            try {
                return kvClient.exists(keys);
            } finally {
                long spend = System.nanoTime() - start;
                KvStorageMonitor.update(namespace, name, method, spend);
            }
        } else {
            return kvClient.exists(keys);
        }
    }

    @Override
    public List<KeyValue> batchGet(byte[]... keys) {
        if (keys.length == 0) {
            return Collections.emptyList();
        }
        if (ProxyMonitorCollector.isMonitorEnable()) {
            long start = System.nanoTime();
            String method = "batchGet_" + keys.length;
            try {
                return kvClient.batchGet(keys);
            } finally {
                long spend = System.nanoTime() - start;
                KvStorageMonitor.update(namespace, name, method, spend);
            }
        } else {
            return kvClient.batchGet(keys);
        }
    }

    @Override
    public void delete(byte[] key) {
        if (ProxyMonitorCollector.isMonitorEnable()) {
            long start = System.nanoTime();
            try {
                kvClient.delete(key);
            } finally {
                long spend = System.nanoTime() - start;
                KvStorageMonitor.update(namespace, name, "delete", spend);
            }
        } else {
            kvClient.delete(key);
        }
    }

    @Override
    public void batchDelete(byte[]... keys) {
        if (keys.length == 0) {
            return;
        }
        if (ProxyMonitorCollector.isMonitorEnable()) {
            long start = System.nanoTime();
            String method = "batchDelete_" + keys.length;
            try {
                kvClient.batchDelete(keys);
            } finally {
                long spend = System.nanoTime() - start;
                KvStorageMonitor.update(namespace, name, method, spend);
            }
        } else {
            kvClient.batchDelete(keys);
        }
    }

    @Override
    public boolean supportCheckAndDelete() {
        return kvClient.supportCheckAndDelete();
    }

    @Override
    public void checkAndDelete(byte[] key, byte[] value) {
        if (ProxyMonitorCollector.isMonitorEnable()) {
            long start = System.nanoTime();
            try {
                kvClient.checkAndDelete(key, value);
            } finally {
                long spend = System.nanoTime() - start;
                KvStorageMonitor.update(namespace, name, "checkAndDelete", spend);
            }
        } else {
            kvClient.checkAndDelete(key, value);
        }
    }

    @Override
    public boolean supportReverseScan() {
        return kvClient.supportReverseScan();
    }

    @Override
    public List<KeyValue> scanByPrefix(byte[] startKey, byte[] prefix, int limit, Sort sort, boolean includeStartKey) {
        if (ProxyMonitorCollector.isMonitorEnable()) {
            long start = System.nanoTime();
            try {
                return kvClient.scanByPrefix(startKey, prefix, limit, sort, includeStartKey);
            } finally {
                long spend = System.nanoTime() - start;
                KvStorageMonitor.update(namespace, name, "scanByPrefix", spend);
            }
        } else {
            return kvClient.scanByPrefix(startKey, prefix, limit, sort, includeStartKey);
        }
    }

    @Override
    public long countByPrefix(byte[] startKey, byte[] prefix, boolean includeStartKey) {
        if (ProxyMonitorCollector.isMonitorEnable()) {
            long start = System.nanoTime();
            try {
                return kvClient.countByPrefix(startKey, prefix, includeStartKey);
            } finally {
                long spend = System.nanoTime() - start;
                KvStorageMonitor.update(namespace, name, "countByPrefix", spend);
            }
        } else {
            return kvClient.countByPrefix(startKey, prefix, includeStartKey);
        }
    }

    @Override
    public List<KeyValue> scanByStartEnd(byte[] startKey, byte[] endKey, byte[] prefix, int limit, Sort sort, boolean includeStartKey) {
        if (ProxyMonitorCollector.isMonitorEnable()) {
            long start = System.nanoTime();
            try {
                return kvClient.scanByStartEnd(startKey, endKey, prefix, limit, sort, includeStartKey);
            } finally {
                long spend = System.nanoTime() - start;
                KvStorageMonitor.update(namespace, name, "scanByStartEnd", spend);
            }
        } else {
            return kvClient.scanByStartEnd(startKey, endKey, prefix, limit, sort, includeStartKey);
        }
    }

    @Override
    public long countByStartEnd(byte[] startKey, byte[] endKey, byte[] prefix, boolean includeStartKey) {
        if (ProxyMonitorCollector.isMonitorEnable()) {
            long start = System.nanoTime();
            try {
                return kvClient.countByStartEnd(startKey, endKey, prefix, includeStartKey);
            } finally {
                long spend = System.nanoTime() - start;
                KvStorageMonitor.update(namespace, name, "countByStartEnd", spend);
            }
        } else {
            return kvClient.countByStartEnd(startKey, endKey, prefix, includeStartKey);
        }
    }
}
