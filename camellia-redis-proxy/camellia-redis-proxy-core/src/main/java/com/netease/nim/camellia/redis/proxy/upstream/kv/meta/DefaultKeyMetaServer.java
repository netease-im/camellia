package com.netease.nim.camellia.redis.proxy.upstream.kv.meta;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.netease.nim.camellia.redis.proxy.reply.*;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.RedisTemplate;
import com.netease.nim.camellia.redis.proxy.upstream.kv.domain.CacheConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.domain.KeyStruct;
import com.netease.nim.camellia.redis.proxy.upstream.kv.exception.KvException;
import com.netease.nim.camellia.redis.proxy.upstream.kv.gc.KvGcExecutor;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.KVClient;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.KeyValue;
import com.netease.nim.camellia.tools.utils.BytesKey;

import java.util.concurrent.CompletableFuture;

/**
 * Created by caojiajun on 2024/4/9
 */
public class DefaultKeyMetaServer implements KeyMetaServer {

    private final ConcurrentLinkedHashMap<BytesKey, Long> delayCacheKeyMap;
    private final KVClient kvClient;
    private final RedisTemplate redisTemplate;
    private final KeyStruct keyStruct;
    private final KvGcExecutor gcExecutor;
    private final CacheConfig cacheConfig;

    public DefaultKeyMetaServer(KVClient kvClient, RedisTemplate redisTemplate, KeyStruct keyStruct, KvGcExecutor gcExecutor, CacheConfig cacheConfig) {
        this.kvClient = kvClient;
        this.redisTemplate = redisTemplate;
        this.keyStruct = keyStruct;
        this.gcExecutor = gcExecutor;
        this.cacheConfig = cacheConfig;
        this.delayCacheKeyMap = new ConcurrentLinkedHashMap.Builder<BytesKey, Long>()
                .initialCapacity(cacheConfig.keyMetaCacheDelayMapSize())
                .maximumWeightedCapacity(cacheConfig.keyMetaCacheDelayMapSize())
                .build();
    }

    @Override
    public KeyMeta getKeyMeta(byte[] key) {
        byte[] metaKey = keyStruct.metaKey(key);

        if (!cacheConfig.isMetaCacheEnable()) {
            KeyValue keyValue = kvClient.get(metaKey);
            if (keyValue == null || keyValue.getValue() == null) {
                return null;
            }
            return KeyMeta.fromBytes(keyValue.getKey());
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
                return keyMeta;
            }
            KeyValue keyValue = kvClient.get(metaKey);
            if (keyValue == null || keyValue.getValue() == null) {
                return null;
            }
            keyMeta = KeyMeta.fromBytes(keyValue.getKey());
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
        byte[] metaKey = keyStruct.metaKey(key);
        Reply reply;
        long redisExpireMillis = redisExpireMillis(keyMeta);
        reply = sync(redisTemplate.sendPSetEx(metaKey, redisExpireMillis, keyMeta.toBytes()));
        delayCacheKeyMap.put(new BytesKey(key), System.currentTimeMillis());

        if (reply instanceof ErrorReply) {
            throw new KvException(((ErrorReply) reply).getError());
        }
        kvClient.put(metaKey, keyMeta.toBytes());
    }

    @Override
    public void deleteKeyMeta(byte[] key) {
        byte[] metaKey = keyStruct.metaKey(key);
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
        byte[] metaKey = keyStruct.metaKey(key);
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
        if (keyMeta == null) {
            return false;
        }
        return !keyMeta.isExpire();
    }

    private Reply sync(CompletableFuture<Reply> future) {
        return redisTemplate.sync(future, cacheConfig.keyMetaTimeoutMillis());
    }
}
