package com.netease.nim.camellia.redis.proxy.upstream.kv.meta;

import com.netease.nim.camellia.redis.proxy.reply.*;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.RedisTemplate;
import com.netease.nim.camellia.redis.proxy.upstream.kv.domain.CacheConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.domain.KeyStruct;
import com.netease.nim.camellia.redis.proxy.upstream.kv.exception.KvException;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.KVClient;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.KeyValue;

import java.util.concurrent.CompletableFuture;

/**
 * Created by caojiajun on 2024/4/9
 */
public class DefaultKeyMetaServer implements KeyMetaServer {

    private final KVClient kvClient;
    private final RedisTemplate keyMetaRedisTemplate;
    private final KeyStruct keyStruct;
    private final CacheConfig cacheConfig;

    public DefaultKeyMetaServer(KVClient kvClient, RedisTemplate keyMetaRedisTemplate, KeyStruct keyStruct, CacheConfig cacheConfig) {
        this.kvClient = kvClient;
        this.keyMetaRedisTemplate = keyMetaRedisTemplate;
        this.keyStruct = keyStruct;
        this.cacheConfig = cacheConfig;
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

        Reply reply = sync(keyMetaRedisTemplate.sendGet(metaKey));
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
                return keyMeta;
            }
            KeyValue keyValue = kvClient.get(metaKey);
            if (keyValue == null || keyValue.getValue() == null) {
                return null;
            }
            keyMeta = KeyMeta.fromBytes(keyValue.getKey());
            if (keyMeta.isExpire()) {
                kvClient.delete(metaKey);
                return null;
            }

            long redisExpireMillis = redisExpireMillis(keyMeta);

            if (redisExpireMillis > 0) {
                Reply reply1 = sync(keyMetaRedisTemplate.sendPSetEx(metaKey, redisExpireMillis, keyMeta.toBytes()));
                if (reply1 instanceof ErrorReply) {
                    throw new KvException(((ErrorReply) reply1).getError());
                }
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
        reply = sync(keyMetaRedisTemplate.sendPSetEx(metaKey, redisExpireMillis, keyMeta.toBytes()));

        if (reply instanceof StatusReply) {
            if (((StatusReply) reply).getStatus().equalsIgnoreCase(StatusReply.OK.getStatus())) {
                return;
            }
        }
        if (reply instanceof ErrorReply) {
            throw new KvException(((ErrorReply) reply).getError());
        }
        kvClient.put(metaKey, keyMeta.toBytes());
    }

    @Override
    public int deleteKeyMeta(byte[] key) {
        byte[] metaKey = keyStruct.metaKey(key);
        int result = 0;
        if (cacheConfig.isMetaCacheEnable()) {
            Reply reply = sync(keyMetaRedisTemplate.sendDel(metaKey));
            if (reply instanceof ErrorReply) {
                throw new KvException(((ErrorReply) reply).getError());
            }
            if (reply instanceof IntegerReply) {
                result = ((IntegerReply) reply).getInteger().intValue();
            }
        }
        if (result > 0) {
            kvClient.delete(metaKey);
            return result;
        }
        KeyValue keyValue = kvClient.get(metaKey);
        if (keyValue == null || keyValue.getValue() == null) {
            return 0;
        }
        KeyMeta keyMeta = KeyMeta.fromBytes(keyValue.getValue());
        kvClient.delete(metaKey);
        return (keyMeta == null || keyMeta.isExpire()) ? 0 : 1;
    }

    @Override
    public boolean existsKeyMeta(byte[] key) {
        byte[] metaKey = keyStruct.metaKey(key);
        if (cacheConfig.isMetaCacheEnable()) {
            Reply reply = sync(keyMetaRedisTemplate.sendExists(metaKey));
            if (reply instanceof ErrorReply) {
                throw new KvException(((ErrorReply) reply).getError());
            }
            if (reply instanceof IntegerReply) {
                return ((IntegerReply) reply).getInteger().intValue() > 0;
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
        return keyMetaRedisTemplate.sync(future, cacheConfig.keyMetaTimeoutMillis());
    }
}
