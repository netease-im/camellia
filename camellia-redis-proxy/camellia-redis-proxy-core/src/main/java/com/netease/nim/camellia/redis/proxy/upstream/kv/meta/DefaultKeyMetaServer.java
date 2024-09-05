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
    public KeyMeta getKeyMeta(int slot, byte[] key) {
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
                deleteKeyMeta(slot, key);
                return null;
            }
        }

        if (cacheConfig.isMetaLocalCacheEnable()) {
            ValueWrapper<KeyMeta> valueWrapper = keyMetaLRUCache.get(slot, key);
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
                    deleteKeyMeta(slot, key);
                    return null;
                }
            }
        }

        byte[] metaKey = keyDesign.metaKey(key);

        KvCacheMonitor.kvStore(cacheConfig.getNamespace(), "getKeyMeta");
        KeyValue keyValue = kvClient.get(slot, metaKey);
        if (keyValue == null || keyValue.getValue() == null) {
            if (cacheConfig.isMetaLocalCacheEnable()) {
                keyMetaLRUCache.setNull(slot, key);
            }
            return null;
        }
        KeyMeta keyMeta = KeyMeta.fromBytes(keyValue.getValue());
        if (keyMeta == null || keyMeta.isExpire()) {
            kvClient.delete(slot, metaKey);
            KvGcMonitor.deleteMetaKeys(cacheConfig.getNamespace(), 1);
            if (cacheConfig.isMetaLocalCacheEnable()) {
                keyMetaLRUCache.setNull(slot, key);
            }
            if (keyMeta != null) {
                gcExecutor.submitSubKeyDeleteTask(slot, key, keyMeta);
            }
            return null;
        }

        if (cacheConfig.isMetaLocalCacheEnable()) {
            keyMetaLRUCache.put(slot, key, keyMeta);
        }
        return keyMeta;
    }

    @Override
    public void createOrUpdateKeyMeta(int slot, byte[] key, KeyMeta keyMeta) {
        Result result = writeBuffer.put(key, keyMeta);

        byte[] metaKey = keyDesign.metaKey(key);
        if (cacheConfig.isMetaLocalCacheEnable()) {
            keyMetaLRUCache.put(slot, key, keyMeta);
        }
        if (!result.isKvWriteDelayEnable()) {
            put(slot, metaKey, keyMeta);
        } else {
            submitAsyncWriteTask(key, result, () -> put(slot, metaKey, keyMeta));
        }
    }

    private void put(int slot, byte[] metaKey, KeyMeta keyMeta) {
        if (keyMeta.getExpireTime() > 0 && kvClient.supportTTL()) {
            long ttl = keyMeta.getExpireTime() - System.currentTimeMillis();
            kvClient.put(slot, metaKey, keyMeta.toBytes(), ttl);
        } else {
            kvClient.put(slot, metaKey, keyMeta.toBytes());
        }
    }

    @Override
    public void deleteKeyMeta(int slot, byte[] key) {
        Result result = writeBuffer.put(key, null);

        byte[] metaKey = keyDesign.metaKey(key);
        if (cacheConfig.isMetaLocalCacheEnable()) {
            keyMetaLRUCache.remove(slot, key);
        }
        if (result.isKvWriteDelayEnable()) {
            submitAsyncWriteTask(key, result, () -> {
                kvClient.delete(slot, metaKey);
                KvGcMonitor.deleteMetaKeys(cacheConfig.getNamespace(), 1);
            });
        } else {
            kvClient.delete(slot, metaKey);
            KvGcMonitor.deleteMetaKeys(cacheConfig.getNamespace(), 1);
        }
    }

    @Override
    public void checkKeyMetaExpired(int slot, byte[] key) {
        byte[] metaKey = keyDesign.metaKey(key);
        KeyValue keyValue = kvClient.get(slot, metaKey);
        if (keyValue == null || keyValue.getValue() == null) {
            return;
        }
        KeyMeta keyMeta = KeyMeta.fromBytes(keyValue.getValue());
        if (keyMeta == null || keyMeta.isExpire()) {
            kvClient.delete(slot, metaKey);
            if (cacheConfig.isMetaLocalCacheEnable()) {
                keyMetaLRUCache.remove(slot, key);
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
