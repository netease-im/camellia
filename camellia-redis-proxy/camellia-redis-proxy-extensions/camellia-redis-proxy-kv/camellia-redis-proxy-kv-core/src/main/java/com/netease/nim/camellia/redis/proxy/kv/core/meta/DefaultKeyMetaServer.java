package com.netease.nim.camellia.redis.proxy.kv.core.meta;

import com.netease.nim.camellia.redis.proxy.kv.core.command.RedisTemplate;
import com.netease.nim.camellia.redis.proxy.kv.core.domain.CacheConfig;
import com.netease.nim.camellia.redis.proxy.kv.core.domain.KeyStruct;
import com.netease.nim.camellia.redis.proxy.kv.core.exception.KvException;
import com.netease.nim.camellia.redis.proxy.reply.*;

import java.util.concurrent.CompletableFuture;

/**
 * Created by caojiajun on 2024/4/9
 */
public class DefaultKeyMetaServer implements KeyMetaServer {

    private final RedisTemplate keyMetaRedisTemplate;
    private final KeyStruct keyStruct;
    private final CacheConfig cacheConfig;

    public DefaultKeyMetaServer(RedisTemplate keyMetaRedisTemplate, KeyStruct keyStruct, CacheConfig cacheConfig) {
        this.keyMetaRedisTemplate = keyMetaRedisTemplate;
        this.keyStruct = keyStruct;
        this.cacheConfig = cacheConfig;
    }

    @Override
    public KeyMeta getKeyMeta(byte[] key) {
        byte[] redisKey = keyStruct.metaKey(key);
        Reply reply = sync(keyMetaRedisTemplate.sendGet(redisKey));
        if (reply instanceof ErrorReply) {
            throw new KvException(((ErrorReply) reply).getError());
        }
        if (reply instanceof BulkReply) {
            byte[] raw = ((BulkReply) reply).getRaw();
            if (raw != null) {
                return KeyMeta.fromBytes(raw);
            }
            return null;
        }
        throw new KvException("ERR key meta error");
    }

    @Override
    public void createOrUpdateKeyMeta(byte[] key, KeyMeta keyMeta) {
        byte[] redisKey = keyStruct.metaKey(key);
        Reply reply;
        if (keyMeta.getExpireTime() > 0) {
            long expireMillis = keyMeta.getExpireTime() - System.currentTimeMillis();
            reply = sync(keyMetaRedisTemplate.sendPSetEx(redisKey, expireMillis, keyMeta.toBytes()));
        } else {
            reply = sync(keyMetaRedisTemplate.sendSet(redisKey, keyMeta.toBytes()));
        }
        if (reply instanceof StatusReply) {
            if (((StatusReply) reply).getStatus().equalsIgnoreCase(StatusReply.OK.getStatus())) {
                return;
            }
        }
        if (reply instanceof ErrorReply) {
            throw new KvException(((ErrorReply) reply).getError());
        }
    }

    @Override
    public int deleteKeyMeta(byte[] key) {
        byte[] redisKey = keyStruct.metaKey(key);
        Reply reply = sync(keyMetaRedisTemplate.sendDel(redisKey));
        if (reply instanceof IntegerReply) {
            return ((IntegerReply) reply).getInteger().intValue();
        }
        if (reply instanceof ErrorReply) {
            throw new KvException(((ErrorReply) reply).getError());
        }
        throw new KvException("ERR internal error");
    }

    @Override
    public boolean existsKeyMeta(byte[] key) {
        byte[] redisKey = keyStruct.metaKey(key);
        Reply reply = sync(keyMetaRedisTemplate.sendExists(redisKey));
        if (reply instanceof IntegerReply) {
            return ((IntegerReply) reply).getInteger().intValue() > 0;
        }
        if (reply instanceof ErrorReply) {
            throw new KvException(((ErrorReply) reply).getError());
        }
        throw new KvException("ERR internal error");
    }

    private Reply sync(CompletableFuture<Reply> future) {
        return keyMetaRedisTemplate.sync(future, cacheConfig.keyMetaTimeoutMillis());
    }
}
