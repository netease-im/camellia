package com.netease.nim.camellia.redis.proxy.upstream.kv.meta;

import com.netease.nim.camellia.redis.proxy.monitor.KvCacheMonitor;
import com.netease.nim.camellia.redis.proxy.monitor.KvGcMonitor;
import com.netease.nim.camellia.redis.proxy.upstream.kv.buffer.WriteBufferValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.KeyMetaLRUCache;
import com.netease.nim.camellia.redis.proxy.upstream.kv.buffer.WriteBuffer;
import com.netease.nim.camellia.redis.proxy.upstream.kv.buffer.Result;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.ValueWrapper;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.KvExecutors;
import com.netease.nim.camellia.redis.proxy.upstream.kv.domain.CacheConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.domain.KeyDesign;
import com.netease.nim.camellia.redis.proxy.upstream.kv.gc.KvGcExecutor;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.KVClient;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.KeyValue;
import com.netease.nim.camellia.redis.proxy.util.MpscSlotHashExecutor;

/**
 * Created by caojiajun on 2024/4/9
 */
public class DefaultKeyMetaServer implements KeyMetaServer {

    private final KeyMetaLRUCache keyMetaLRUCache;
    private final WriteBuffer<KeyMeta> writeBuffer;
    private final MpscSlotHashExecutor asyncWriteExecutor;
    private final KVClient kvClient;
    private final KeyDesign keyDesign;
    private final KvGcExecutor gcExecutor;
    private final CacheConfig cacheConfig;

    public DefaultKeyMetaServer(KVClient kvClient, KeyDesign keyDesign, KvGcExecutor gcExecutor, CacheConfig cacheConfig) {
        this.kvClient = kvClient;
        this.keyDesign = keyDesign;
        this.gcExecutor = gcExecutor;
        this.cacheConfig = cacheConfig;
        this.asyncWriteExecutor = KvExecutors.getInstance().getAsyncWriteExecutor();
        this.keyMetaLRUCache = cacheConfig.getKeyMetaLRUCache();
        this.writeBuffer = WriteBuffer.newWriteBuffer(cacheConfig.getNamespace(), "key.meta");
    }

    @Override
    public KeyMeta getKeyMeta(byte[] key) {
        WriteBufferValue<KeyMeta> writeBufferValue = writeBuffer.get(key);
        if (writeBufferValue != null) {
            KvCacheMonitor.writeBuffer(cacheConfig.getNamespace(), "getKeyMeta");
            KeyMeta keyMeta = writeBufferValue.getValue();
            if (keyMeta == null) {
                return null;
            }
            if (!keyMeta.isExpire()) {
                return keyMeta;
            } else {
                deleteKeyMeta(key);
                return null;
            }
        }

        if (cacheConfig.isMetaLocalCacheEnable()) {
            ValueWrapper<KeyMeta> valueWrapper = keyMetaLRUCache.get(key);
            if (valueWrapper != null) {
                KeyMeta keyMeta = valueWrapper.get();
                if (keyMeta == null) {
                    KvCacheMonitor.localCache(cacheConfig.getNamespace(), "getKeyMeta");
                    return null;
                }
                if (!keyMeta.isExpire()) {
                    KvCacheMonitor.localCache(cacheConfig.getNamespace(), "getKeyMeta");
                    return keyMeta;
                } else {
                    deleteKeyMeta(key);
                    return null;
                }
            }
        }

        byte[] metaKey = keyDesign.metaKey(key);

        KvCacheMonitor.kvStore(cacheConfig.getNamespace(), "getKeyMeta");
        KeyValue keyValue = kvClient.get(metaKey);
        if (keyValue == null || keyValue.getValue() == null) {
            keyMetaLRUCache.setNull(key);
            return null;
        }
        KeyMeta keyMeta = KeyMeta.fromBytes(keyValue.getValue());
        if (keyMeta == null || keyMeta.isExpire()) {
            kvClient.delete(metaKey);
            KvGcMonitor.deleteMetaKeys(cacheConfig.getNamespace(), 1);
            keyMetaLRUCache.setNull(key);
            if (keyMeta != null) {
                gcExecutor.submitSubKeyDeleteTask(key, keyMeta);
            }
            return null;
        }

        if (cacheConfig.isMetaLocalCacheEnable()) {
            keyMetaLRUCache.put(key, keyMeta);
        }
        return keyMeta;
    }

    @Override
    public void createOrUpdateKeyMeta(byte[] key, KeyMeta keyMeta) {
        Result result = writeBuffer.put(key, keyMeta);

        byte[] metaKey = keyDesign.metaKey(key);
        if (cacheConfig.isMetaLocalCacheEnable()) {
            keyMetaLRUCache.put(key, keyMeta);
        }
        if (!result.isKvWriteDelayEnable()) {
            put(metaKey, keyMeta);
        } else {
            submitAsyncWriteTask(key, result, () -> put(metaKey, keyMeta));
        }
    }

    private void put(byte[] metaKey, KeyMeta keyMeta) {
        if (keyMeta.getExpireTime() > 0 && kvClient.supportTTL()) {
            long ttl = keyMeta.getExpireTime() - System.currentTimeMillis();
            kvClient.put(metaKey, keyMeta.toBytes(), ttl);
        } else {
            kvClient.put(metaKey, keyMeta.toBytes());
        }
    }

    @Override
    public void deleteKeyMeta(byte[] key) {
        Result result = writeBuffer.put(key, null);

        byte[] metaKey = keyDesign.metaKey(key);
        if (cacheConfig.isMetaLocalCacheEnable()) {
            keyMetaLRUCache.remove(key);
        }
        if (result.isKvWriteDelayEnable()) {
            submitAsyncWriteTask(key, result, () -> {
                kvClient.delete(metaKey);
                KvGcMonitor.deleteMetaKeys(cacheConfig.getNamespace(), 1);
            });
        } else {
            kvClient.delete(metaKey);
            KvGcMonitor.deleteMetaKeys(cacheConfig.getNamespace(), 1);
        }
    }

    @Override
    public boolean existsKeyMeta(byte[] key) {
        WriteBufferValue<KeyMeta> writeBufferValue = writeBuffer.get(key);
        if (writeBufferValue != null) {
            KvCacheMonitor.writeBuffer(cacheConfig.getNamespace(), "existsKeyMeta");
            KeyMeta keyMeta = writeBufferValue.getValue();
            if (keyMeta == null) {
                return false;
            }
            if (!keyMeta.isExpire()) {
                return true;
            } else {
                deleteKeyMeta(key);
                return false;
            }
        }

        if (cacheConfig.isMetaLocalCacheEnable()) {
            ValueWrapper<KeyMeta> valueWrapper = keyMetaLRUCache.get(key);
            if (valueWrapper != null) {
                KeyMeta keyMeta = valueWrapper.get();
                if (keyMeta == null) {
                    KvCacheMonitor.localCache(cacheConfig.getNamespace(), "existsKeyMeta");
                    return false;
                }
                if (!keyMeta.isExpire()) {
                    KvCacheMonitor.localCache(cacheConfig.getNamespace(), "existsKeyMeta");
                    return true;
                } else {
                    deleteKeyMeta(key);
                    return false;
                }
            }
        }

        byte[] metaKey = keyDesign.metaKey(key);
        KvCacheMonitor.kvStore(cacheConfig.getNamespace(), "existsKeyMeta");
        KeyValue keyValue = kvClient.get(metaKey);
        if (keyValue == null || keyValue.getValue() == null) {
            return false;
        }
        KeyMeta keyMeta = KeyMeta.fromBytes(keyValue.getValue());
        if (keyMeta == null || keyMeta.isExpire()) {
            kvClient.delete(metaKey);
            KvGcMonitor.deleteMetaKeys(cacheConfig.getNamespace(), 1);
            return false;
        }
        return true;
    }

    @Override
    public void checkKeyMetaExpired(byte[] key) {
        byte[] metaKey = keyDesign.metaKey(key);
        KeyValue keyValue = kvClient.get(metaKey);
        if (keyValue == null || keyValue.getValue() == null) {
            return;
        }
        KeyMeta keyMeta = KeyMeta.fromBytes(keyValue.getValue());
        if (keyMeta == null || keyMeta.isExpire()) {
            kvClient.delete(metaKey);
            if (cacheConfig.isMetaLocalCacheEnable()) {
                keyMetaLRUCache.remove(key);
            }
        }
    }

    private void submitAsyncWriteTask(byte[] key, Result result, Runnable runnable) {
        try {
            asyncWriteExecutor.submit(key, () -> {
                try {
                    runnable.run();
                } finally {
                    result.kvWriteDone();
                }
            });
        } catch (Exception e) {
            result.kvWriteDone();
            throw e;
        }
    }
}
