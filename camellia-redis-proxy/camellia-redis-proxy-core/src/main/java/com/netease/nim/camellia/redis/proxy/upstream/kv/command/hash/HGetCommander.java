package com.netease.nim.camellia.redis.proxy.upstream.kv.command.hash;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.monitor.KvCacheMonitor;
import com.netease.nim.camellia.redis.proxy.reply.BulkReply;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.upstream.kv.buffer.WriteBufferValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.RedisHash;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.HashLRUCache;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.ValueWrapper;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.CommanderConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.KeyValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyType;
import com.netease.nim.camellia.tools.utils.BytesKey;

/**
 * HGET key field
 * <p>
 * Created by caojiajun on 2024/4/7
 */
public class HGetCommander extends Hash0Commander {

    public HGetCommander(CommanderConfig commanderConfig) {
        super(commanderConfig);
    }

    @Override
    public RedisCommand redisCommand() {
        return RedisCommand.HGET;
    }

    @Override
    protected boolean parse(Command command) {
        byte[][] objects = command.getObjects();
        return objects.length == 3;
    }

    @Override
    public Reply runToCompletion(int slot, Command command) {
        byte[][] objects = command.getObjects();
        byte[] key = objects[1];
        byte[] field = objects[2];

        //meta
        ValueWrapper<KeyMeta> valueWrapper = keyMetaServer.runToComplete(slot, key);
        if (valueWrapper == null) {
            return null;
        }
        KeyMeta keyMeta = valueWrapper.get();
        if (keyMeta == null) {
            return BulkReply.NIL_REPLY;
        }
        if (keyMeta.getKeyType() != KeyType.hash) {
            return ErrorReply.WRONG_TYPE;
        }

        byte[] cacheKey = keyDesign.cacheKey(keyMeta, key);

        WriteBufferValue<RedisHash> writeBufferValue = hashWriteBuffer.get(cacheKey);
        if (writeBufferValue != null) {
            byte[] bytes = writeBufferValue.getValue().hget(new BytesKey(field));
            KvCacheMonitor.writeBuffer(cacheConfig.getNamespace(), redisCommand().strRaw());
            return new BulkReply(bytes);
        }

        if (cacheConfig.isHashLocalCacheEnable()) {
            HashLRUCache hashLRUCache = cacheConfig.getHashLRUCache();

            RedisHash hash = hashLRUCache.getForRead(slot, cacheKey);
            if (hash != null) {
                KvCacheMonitor.localCache(cacheConfig.getNamespace(), redisCommand().strRaw());
                return new BulkReply(hash.hget(new BytesKey(field)));
            }
        }

        return null;
    }

    @Override
    protected Reply execute(int slot, Command command) {
        byte[][] objects = command.getObjects();
        byte[] key = objects[1];
        byte[] field = objects[2];

        //meta
        KeyMeta keyMeta = keyMetaServer.getKeyMeta(slot, key);
        if (keyMeta == null) {
            return BulkReply.NIL_REPLY;
        }
        if (keyMeta.getKeyType() != KeyType.hash) {
            return ErrorReply.WRONG_TYPE;
        }

        byte[] cacheKey = keyDesign.cacheKey(keyMeta, key);

        WriteBufferValue<RedisHash> writeBufferValue = hashWriteBuffer.get(cacheKey);
        if (writeBufferValue != null) {
            byte[] bytes = writeBufferValue.getValue().hget(new BytesKey(field));
            KvCacheMonitor.writeBuffer(cacheConfig.getNamespace(), redisCommand().strRaw());
            return new BulkReply(bytes);
        }

        if (cacheConfig.isHashLocalCacheEnable()) {
            HashLRUCache hashLRUCache = cacheConfig.getHashLRUCache();

            RedisHash hash = hashLRUCache.getForRead(slot, cacheKey);
            if (hash != null) {
                KvCacheMonitor.localCache(cacheConfig.getNamespace(), redisCommand().strRaw());
                return new BulkReply(hash.hget(new BytesKey(field)));
            }

            boolean hotKey = hashLRUCache.isHotKey(key, redisCommand());
            if (hotKey) {
                hash = loadLRUCache(slot, keyMeta, key);
                hashLRUCache.putAllForRead(slot, cacheKey, hash);
                KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());
                return new BulkReply(hash.hget(new BytesKey(field)));
            }
        }

        KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());
        byte[] subKey = keyDesign.hashFieldSubKey(keyMeta, key, field);
        KeyValue keyValue = kvClient.get(slot, subKey);
        if (keyValue == null || keyValue.getValue() == null) {
            return BulkReply.NIL_REPLY;
        }
        return new BulkReply(keyValue.getValue());
    }

}
