package com.netease.nim.camellia.redis.proxy.upstream.kv.meta;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.netease.nim.camellia.redis.proxy.monitor.KVCacheMonitor;
import com.netease.nim.camellia.redis.proxy.reply.*;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.KeyMetaLRUCache;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.RedisTemplate;
import com.netease.nim.camellia.redis.proxy.upstream.kv.domain.CacheConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.domain.KeyDesign;
import com.netease.nim.camellia.redis.proxy.upstream.kv.exception.KvException;
import com.netease.nim.camellia.redis.proxy.upstream.kv.gc.KvGcExecutor;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.KVClient;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.KeyValue;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import com.netease.nim.camellia.tools.utils.BytesKey;

import java.util.concurrent.CompletableFuture;

/**
 * Created by caojiajun on 2024/4/9
 */
public class DefaultKeyMetaServer implements KeyMetaServer {

    private final KeyMetaLRUCache keyMetaLRUCache;
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
        this.keyMetaLRUCache = cacheConfig.getKeyMetaLRUCache();
    }

    @Override
    public KeyMeta getKeyMeta(byte[] key) {
        if (cacheConfig.isMetaLocalCacheEnable()) {
            KeyMeta keyMeta = keyMetaLRUCache.get(key);
            if (keyMeta != null && keyMeta.isExpire()) {
                keyMetaLRUCache.remove(key);
                keyMeta = null;
            }
            if (keyMeta != null) {
                KVCacheMonitor.localCache(cacheConfig.getNamespace(), "key_meta");
                return keyMeta;
            }
        }

        byte[] metaKey = keyDesign.metaKey(key);

        if (!cacheConfig.isMetaCacheEnable()) {
            KVCacheMonitor.kvStore(cacheConfig.getNamespace(), "key_meta");
            KeyValue keyValue = kvClient.get(metaKey);
            if (keyValue == null || keyValue.getValue() == null) {
                return null;
            }
            KeyMeta keyMeta = KeyMeta.fromBytes(keyValue.getValue());
            if (keyMeta == null || keyMeta.isExpire()) {
                kvClient.delete(metaKey);
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
                KVCacheMonitor.redisCache(cacheConfig.getNamespace(), "key_meta");
                return keyMeta;
            }
            KVCacheMonitor.kvStore(cacheConfig.getNamespace(), "key_meta");

            KeyValue keyValue = kvClient.get(metaKey);
            if (keyValue == null || keyValue.getValue() == null) {
                return null;
            }
            keyMeta = KeyMeta.fromBytes(keyValue.getValue());
            if (keyMeta == null || keyMeta.isExpire()) {
                kvClient.delete(metaKey);
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
        return redisExpireMillis;
    }

    @Override
    public void createOrUpdateKeyMeta(byte[] key, KeyMeta keyMeta) {
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
        if (keyMeta.getExpireTime() > 0 && kvClient.supportTTL()) {
            long ttl = keyMeta.getExpireTime() - System.currentTimeMillis();
            kvClient.put(metaKey, keyMeta.toBytes(), ttl);
        } else {
            kvClient.put(metaKey, keyMeta.toBytes());
        }
    }

    @Override
    public void deleteKeyMeta(byte[] key) {
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
        kvClient.delete(metaKey);
    }

    @Override
    public boolean existsKeyMeta(byte[] key) {
        if (cacheConfig.isMetaLocalCacheEnable()) {
            KeyMeta keyMeta = keyMetaLRUCache.get(key);
            if (keyMeta != null) {
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
                    return true;
                }
            }
        }
        KeyValue keyValue = kvClient.get(metaKey);
        if (keyValue == null || keyValue.getValue() == null) {
            return false;
        }
        KeyMeta keyMeta = KeyMeta.fromBytes(keyValue.getValue());
        if (keyMeta == null || keyMeta.isExpire()) {
            kvClient.delete(metaKey);
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
}
