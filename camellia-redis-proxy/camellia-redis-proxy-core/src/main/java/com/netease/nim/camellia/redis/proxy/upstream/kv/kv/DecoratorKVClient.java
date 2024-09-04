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
    public void put(int slot, byte[] key, byte[] value, long ttl) {
        if (ProxyMonitorCollector.isMonitorEnable()) {
            long start = System.nanoTime();
            try {
                kvClient.put(slot, key, value, ttl);
            } finally {
                long spend = System.nanoTime() - start;
                KvStorageMonitor.update(namespace, name, "putWithTTL", spend);
            }
        } else {
            kvClient.put(slot, key, value, ttl);
        }
    }

    @Override
    public void put(int slot, byte[] key, byte[] value) {
        if (ProxyMonitorCollector.isMonitorEnable()) {
            long start = System.nanoTime();
            try {
                kvClient.put(slot, key, value);
            } finally {
                long spend = System.nanoTime() - start;
                KvStorageMonitor.update(namespace, name, "put", spend);
            }
        } else {
            kvClient.put(slot, key, value);
        }
    }

    @Override
    public void batchPut(int slot, List<KeyValue> list) {
        if (list.isEmpty()) {
            return;
        }
        if (ProxyMonitorCollector.isMonitorEnable()) {
            long start = System.nanoTime();
            String method = "batchPut_" + list.size();
            try {
                kvClient.batchPut(slot, list);
            } finally {
                long spend = System.nanoTime() - start;
                KvStorageMonitor.update(namespace, name, method, spend);
            }
        } else {
            kvClient.batchPut(slot, list);
        }
    }

    @Override
    public KeyValue get(int slot, byte[] key) {
        if (ProxyMonitorCollector.isMonitorEnable()) {
            long start = System.nanoTime();
            try {
                return kvClient.get(slot, key);
            } finally {
                long spend = System.nanoTime() - start;
                KvStorageMonitor.update(namespace, name, "get", spend);
            }
        } else {
            return kvClient.get(slot, key);
        }
    }

    @Override
    public boolean exists(int slot, byte[] key) {
        if (ProxyMonitorCollector.isMonitorEnable()) {
            long start = System.nanoTime();
            try {
                return kvClient.exists(slot, key);
            } finally {
                long spend = System.nanoTime() - start;
                KvStorageMonitor.update(namespace, name, "exists", spend);
            }
        } else {
            return kvClient.exists(slot, key);
        }
    }

    @Override
    public boolean[] exists(int slot, byte[]... keys) {
        if (keys.length == 0) {
            return new boolean[0];
        }
        if (ProxyMonitorCollector.isMonitorEnable()) {
            long start = System.nanoTime();
            String method = "exists_" + keys.length;
            try {
                return kvClient.exists(slot, keys);
            } finally {
                long spend = System.nanoTime() - start;
                KvStorageMonitor.update(namespace, name, method, spend);
            }
        } else {
            return kvClient.exists(slot, keys);
        }
    }

    @Override
    public List<KeyValue> batchGet(int slot, byte[]... keys) {
        if (keys.length == 0) {
            return Collections.emptyList();
        }
        if (ProxyMonitorCollector.isMonitorEnable()) {
            long start = System.nanoTime();
            String method = "batchGet_" + keys.length;
            try {
                return kvClient.batchGet(slot, keys);
            } finally {
                long spend = System.nanoTime() - start;
                KvStorageMonitor.update(namespace, name, method, spend);
            }
        } else {
            return kvClient.batchGet(slot, keys);
        }
    }

    @Override
    public void delete(int slot, byte[] key) {
        if (ProxyMonitorCollector.isMonitorEnable()) {
            long start = System.nanoTime();
            try {
                kvClient.delete(slot, key);
            } finally {
                long spend = System.nanoTime() - start;
                KvStorageMonitor.update(namespace, name, "delete", spend);
            }
        } else {
            kvClient.delete(slot, key);
        }
    }

    @Override
    public void batchDelete(int slot, byte[]... keys) {
        if (keys.length == 0) {
            return;
        }
        if (ProxyMonitorCollector.isMonitorEnable()) {
            long start = System.nanoTime();
            String method = "batchDelete_" + keys.length;
            try {
                kvClient.batchDelete(slot, keys);
            } finally {
                long spend = System.nanoTime() - start;
                KvStorageMonitor.update(namespace, name, method, spend);
            }
        } else {
            kvClient.batchDelete(slot, keys);
        }
    }

    @Override
    public boolean supportCheckAndDelete() {
        return kvClient.supportCheckAndDelete();
    }

    @Override
    public void checkAndDelete(int slot, byte[] key, byte[] value) {
        if (ProxyMonitorCollector.isMonitorEnable()) {
            long start = System.nanoTime();
            try {
                kvClient.checkAndDelete(slot, key, value);
            } finally {
                long spend = System.nanoTime() - start;
                KvStorageMonitor.update(namespace, name, "checkAndDelete", spend);
            }
        } else {
            kvClient.checkAndDelete(slot, key, value);
        }
    }

    @Override
    public boolean supportReverseScan() {
        return kvClient.supportReverseScan();
    }

    @Override
    public List<KeyValue> scanByPrefix(int slot, byte[] startKey, byte[] prefix, int limit, Sort sort, boolean includeStartKey) {
        if (ProxyMonitorCollector.isMonitorEnable()) {
            long start = System.nanoTime();
            try {
                return kvClient.scanByPrefix(slot, startKey, prefix, limit, sort, includeStartKey);
            } finally {
                long spend = System.nanoTime() - start;
                KvStorageMonitor.update(namespace, name, "scanByPrefix", spend);
            }
        } else {
            return kvClient.scanByPrefix(slot, startKey, prefix, limit, sort, includeStartKey);
        }
    }

    @Override
    public long countByPrefix(int slot, byte[] startKey, byte[] prefix, boolean includeStartKey) {
        if (ProxyMonitorCollector.isMonitorEnable()) {
            long start = System.nanoTime();
            try {
                return kvClient.countByPrefix(slot, startKey, prefix, includeStartKey);
            } finally {
                long spend = System.nanoTime() - start;
                KvStorageMonitor.update(namespace, name, "countByPrefix", spend);
            }
        } else {
            return kvClient.countByPrefix(slot, startKey, prefix, includeStartKey);
        }
    }

    @Override
    public List<KeyValue> scanByStartEnd(int slot, byte[] startKey, byte[] endKey, byte[] prefix, int limit, Sort sort, boolean includeStartKey) {
        if (ProxyMonitorCollector.isMonitorEnable()) {
            long start = System.nanoTime();
            try {
                return kvClient.scanByStartEnd(slot, startKey, endKey, prefix, limit, sort, includeStartKey);
            } finally {
                long spend = System.nanoTime() - start;
                KvStorageMonitor.update(namespace, name, "scanByStartEnd", spend);
            }
        } else {
            return kvClient.scanByStartEnd(slot, startKey, endKey, prefix, limit, sort, includeStartKey);
        }
    }

    @Override
    public long countByStartEnd(int slot, byte[] startKey, byte[] endKey, byte[] prefix, boolean includeStartKey) {
        if (ProxyMonitorCollector.isMonitorEnable()) {
            long start = System.nanoTime();
            try {
                return kvClient.countByStartEnd(slot, startKey, endKey, prefix, includeStartKey);
            } finally {
                long spend = System.nanoTime() - start;
                KvStorageMonitor.update(namespace, name, "countByStartEnd", spend);
            }
        } else {
            return kvClient.countByStartEnd(slot, startKey, endKey, prefix, includeStartKey);
        }
    }
}
