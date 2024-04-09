package com.netease.nim.camellia.redis.proxy.kv.core.meta;

import com.netease.nim.camellia.redis.proxy.kv.core.command.RedisTemplate;
import com.netease.nim.camellia.redis.proxy.kv.core.domain.CacheConfig;
import com.netease.nim.camellia.redis.proxy.kv.core.domain.KeyStruct;
import com.netease.nim.camellia.redis.proxy.kv.core.exception.KvException;
import com.netease.nim.camellia.redis.proxy.reply.BulkReply;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.reply.StatusReply;

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
    public KeyMeta getKeyMeta(byte[] key, KeyType keyType, boolean createIfNotExists) {
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
            if (!createIfNotExists) {
                return null;
            }
            if (keyType == null) {
                throw new KvException("ERR key meta error");
            }
            KeyMeta keyMeta = new KeyMeta(keyType, System.currentTimeMillis(), -1);
            Reply reply1 = sync(keyMetaRedisTemplate.sendSet(redisKey, keyMeta.toBytes()));
            if (reply1 instanceof StatusReply) {
                if (((StatusReply) reply1).getStatus().equalsIgnoreCase(StatusReply.OK.getStatus())) {
                    return keyMeta;
                }
            }
            if (reply1 instanceof ErrorReply) {
                throw new KvException(((ErrorReply) reply1).getError());
            }
        }
        throw new KvException("ERR key meta error");
    }

    @Override
    public void createOrUpdateKeyMeta(byte[] key, KeyMeta keyMeta) {
        byte[] redisKey = keyStruct.metaKey(key);
        long expireMillis = keyMeta.getExpireTime() - System.currentTimeMillis();
        Reply reply = sync(keyMetaRedisTemplate.sendPSetEx(redisKey, expireMillis, keyMeta.toBytes()));
        if (reply instanceof StatusReply) {
            if (((StatusReply) reply).getStatus().equalsIgnoreCase(StatusReply.OK.getStatus())) {
                return;
            }
        }
        if (reply instanceof ErrorReply) {
            throw new KvException(((ErrorReply) reply).getError());
        }
    }

    private Reply sync(CompletableFuture<Reply> future) {
        return keyMetaRedisTemplate.sync(future, cacheConfig.keyMetaTimeoutMillis());
    }
}
