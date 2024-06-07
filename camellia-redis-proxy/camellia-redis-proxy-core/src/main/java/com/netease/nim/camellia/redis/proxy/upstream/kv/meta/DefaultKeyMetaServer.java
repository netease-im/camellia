package com.netease.nim.camellia.redis.proxy.upstream.kv.meta;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.netease.nim.camellia.redis.proxy.monitor.KvCacheMonitor;
import com.netease.nim.camellia.redis.proxy.monitor.KvGcMonitor;
import com.netease.nim.camellia.redis.proxy.reply.*;
import com.netease.nim.camellia.redis.proxy.upstream.kv.buffer.WriteBufferValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.KeyMetaLRUCache;
import com.netease.nim.camellia.redis.proxy.upstream.kv.buffer.WriteBuffer;
import com.netease.nim.camellia.redis.proxy.upstream.kv.buffer.Result;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.KvExecutors;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.RedisTemplate;
import com.netease.nim.camellia.redis.proxy.upstream.kv.domain.CacheConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.domain.KeyDesign;
import com.netease.nim.camellia.redis.proxy.upstream.kv.exception.KvException;
import com.netease.nim.camellia.redis.proxy.upstream.kv.gc.KvGcExecutor;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.KVClient;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.KeyValue;
import com.netease.nim.camellia.redis.proxy.util.MpscHashedExecutor;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import com.netease.nim.camellia.tools.utils.BytesKey;

import java.util.concurrent.CompletableFuture;

/**
 * Created by caojiajun on 2024/4/9
 */
public class DefaultKeyMetaServer implements KeyMetaServer {

    private final KeyMetaLRUCache keyMetaLRUCache;
    private final WriteBuffer<KeyMeta> writeBuffer;
    private final MpscHashedExecutor asyncWriteExecutor;
    private final ConcurrentLinkedHashMap<BytesKey, Long> delayCacheKeyMap;
    private final KVClient kvClient;
    private final RedisTemplate redisTemplate;
    private final KeyDesign keyDesign;
    private final KvGcExecutor gcExecutor;
    private final CacheConfig cacheConfig;

    public DefaultKeyMetaServer(KVClient kvClient, RedisTemplate redisTemplate, KeyDesign keyDesign, KvGcExecutor gcExecutor, CacheConfig cacheConfig) {
        this.kvClient = kvClient;
        this.redisTemplate = redisTemplate;
        this.keyDesign = keyDesign;
        this.gcExecutor = gcExecutor;
        this.cacheConfig = cacheConfig;
        this.delayCacheKeyMap = new ConcurrentLinkedHashMap.Builder<BytesKey, Long>()
                .initialCapacity(cacheConfig.keyMetaCacheDelayMapSize())
                .maximumWeightedCapacity(cacheConfig.keyMetaCacheDelayMapSize())
                .build();
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
            KeyMeta keyMeta = keyMetaLRUCache.get(key);
            if (keyMeta != null && keyMeta.isExpire()) {
                keyMetaLRUCache.remove(key);
                keyMeta = null;
            }
            if (keyMeta != null) {
                KvCacheMonitor.localCache(cacheConfig.getNamespace(), "getKeyMeta");
                return keyMeta;
            }
        }

        byte[] metaKey = keyDesign.metaKey(key);

        if (!cacheConfig.isMetaCacheEnable()) {
            KvCacheMonitor.kvStore(cacheConfig.getNamespace(), "getKeyMeta");
            KeyValue keyValue = kvClient.get(metaKey);
            if (keyValue == null || keyValue.getValue() == null) {
                return null;
            }
            KeyMeta keyMeta = KeyMeta.fromBytes(keyValue.getValue());
            if (keyMeta == null || keyMeta.isExpire()) {
                kvClient.delete(metaKey);
                KvGcMonitor.deleteMetaKeys(cacheConfig.getNamespace(), 1);
                return null;
            }
            if (cacheConfig.isMetaLocalCacheEnable()) {
                keyMetaLRUCache.put(key, keyMeta);
            }
            return keyMeta;
        }

        Reply reply = sync(redisTemplate.sendGet(metaKey));
        if (reply instanceof ErrorReply) {
            throw new KvException(((ErrorReply) reply).getError());
        }
        if (reply instanceof BulkReply) {
            byte[] raw = ((BulkReply) reply).getRaw();
            KeyMeta keyMeta = null;
            if (raw != null) {
                keyMeta = KeyMeta.fromBytes(raw);
            }
            if (keyMeta != null) {
                BytesKey bytesKey = new BytesKey(key);
                Long lastDelayTime = delayCacheKeyMap.get(bytesKey);
                if (lastDelayTime == null || System.currentTimeMillis() - lastDelayTime > cacheConfig.keyMetaCacheKeyDelayMinIntervalSeconds()*1000L) {
                    long redisExpireMillis = redisExpireMillis(keyMeta);
                    if (redisExpireMillis > 0) {
                        redisTemplate.sendPSetEx(metaKey, redisExpireMillis, keyMeta.toBytes());
                        delayCacheKeyMap.put(bytesKey, System.currentTimeMillis());
                    }
                }
                if (cacheConfig.isMetaLocalCacheEnable()) {
                    keyMetaLRUCache.put(key, keyMeta);
                }
                KvCacheMonitor.redisCache(cacheConfig.getNamespace(), "getKeyMeta");
                return keyMeta;
            }
            KvCacheMonitor.kvStore(cacheConfig.getNamespace(), "getKeyMeta");

            KeyValue keyValue = kvClient.get(metaKey);
            if (keyValue == null || keyValue.getValue() == null) {
                return null;
            }
            keyMeta = KeyMeta.fromBytes(keyValue.getValue());
            if (keyMeta == null || keyMeta.isExpire()) {
                kvClient.delete(metaKey);
                KvGcMonitor.deleteMetaKeys(cacheConfig.getNamespace(), 1);
                if (keyMeta != null) {
                    gcExecutor.submitSubKeyDeleteTask(key, keyMeta);
                }
                return null;
            }

            long redisExpireMillis = redisExpireMillis(keyMeta);

            if (redisExpireMillis > 0) {
                Reply reply1 = sync(redisTemplate.sendPSetEx(metaKey, redisExpireMillis, keyMeta.toBytes()));
                if (reply1 instanceof ErrorReply) {
                    throw new KvException(((ErrorReply) reply1).getError());
                }
                delayCacheKeyMap.put(new BytesKey(key), System.currentTimeMillis());
            }
            if (cacheConfig.isMetaLocalCacheEnable()) {
                keyMetaLRUCache.put(key, keyMeta);
            }
            return keyMeta;
        }
        throw new KvException("ERR key meta error");
    }

    private long redisExpireMillis(KeyMeta keyMeta) {
        long redisExpireMillis;
        if (keyMeta.getExpireTime() < 0) {
            redisExpireMillis = cacheConfig.metaCacheMillis();
        } else {
            redisExpireMillis = keyMeta.getExpireTime() - System.currentTimeMillis();
            redisExpireMillis = Math.min(redisExpireMillis, cacheConfig.metaCacheMillis());
        }
        if (redisExpireMillis <= 0) {
            redisExpireMillis = 1;
        }
        return redisExpireMillis;
    }

    @Override
    public void createOrUpdateKeyMeta(byte[] key, KeyMeta keyMeta) {
        Result result = writeBuffer.put(key, keyMeta);

        byte[] metaKey = keyDesign.metaKey(key);
        if (cacheConfig.isMetaLocalCacheEnable()) {
            keyMetaLRUCache.put(key, keyMeta);
        }
        if (cacheConfig.isMetaCacheEnable()) {
            Reply reply;
            long redisExpireMillis = redisExpireMillis(keyMeta);
            reply = sync(redisTemplate.sendPSetEx(metaKey, redisExpireMillis, keyMeta.toBytes()));
            delayCacheKeyMap.put(new BytesKey(key), System.currentTimeMillis());

            if (reply instanceof ErrorReply) {
                throw new KvException(((ErrorReply) reply).getError());
            }
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
        if (cacheConfig.isMetaCacheEnable()) {
            Reply reply = sync(redisTemplate.sendDel(metaKey));
            if (reply instanceof ErrorReply) {
                throw new KvException(((ErrorReply) reply).getError());
            }
            delayCacheKeyMap.remove(new BytesKey(key));
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
            KeyMeta keyMeta = keyMetaLRUCache.get(key);
            if (keyMeta != null) {
                KvCacheMonitor.localCache(cacheConfig.getNamespace(), "existsKeyMeta");
                return !keyMeta.isExpire();
            }
        }

        byte[] metaKey = keyDesign.metaKey(key);
        if (cacheConfig.isMetaCacheEnable()) {
            Reply reply = sync(redisTemplate.sendExists(metaKey));
            if (reply instanceof ErrorReply) {
                throw new KvException(((ErrorReply) reply).getError());
            }
            if (reply instanceof IntegerReply) {
                if (((IntegerReply) reply).getInteger().intValue() > 0) {
                    KvCacheMonitor.redisCache(cacheConfig.getNamespace(), "existsKeyMeta");
                    return true;
                }
            }
        }
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
            if (cacheConfig.isMetaCacheEnable()) {
                Reply reply = sync(redisTemplate.sendDel(metaKey));
                if (reply instanceof ErrorReply) {
                    ErrorLogCollector.collect(DefaultKeyMetaServer.class, "checkKeyMetaExpired error, error = " + ((ErrorReply) reply).getError());
                }
            }
        }
    }

    private Reply sync(CompletableFuture<Reply> future) {
        return redisTemplate.sync(future, cacheConfig.keyMetaTimeoutMillis());
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
