package com.netease.nim.camellia.redis.proxy.upstream.kv.command.hash;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.monitor.KvCacheMonitor;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.IntegerReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.upstream.kv.buffer.WriteBufferValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.HashLRUCache;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.RedisHash;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.CommanderConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.EncodeVersion;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyType;
import com.netease.nim.camellia.redis.proxy.upstream.kv.utils.BytesUtils;

/**
 * HLEN key
 * <p>
 * Created by caojiajun on 2024/4/11
 */
public class HLenCommander extends Hash0Commander {

    public HLenCommander(CommanderConfig commanderConfig) {
        super(commanderConfig);
    }

    @Override
    public RedisCommand redisCommand() {
        return RedisCommand.HLEN;
    }

    @Override
    protected boolean parse(Command command) {
        byte[][] objects = command.getObjects();
        return objects.length == 2;
    }

    @Override
    protected Reply execute(Command command) {
        byte[][] objects = command.getObjects();
        byte[] key = objects[1];
        KeyMeta keyMeta = keyMetaServer.getKeyMeta(key);
        if (keyMeta == null) {
            return IntegerReply.REPLY_0;
        }
        if (keyMeta.getKeyType() != KeyType.hash) {
            return ErrorReply.WRONG_TYPE;
        }

        byte[] cacheKey = keyDesign.cacheKey(keyMeta, key);

        Reply reply = getSizeFromCache(keyMeta, key, cacheKey);
        if (reply != null) {
            return reply;
        }
        EncodeVersion encodeVersion = keyMeta.getEncodeVersion();
        if (encodeVersion == EncodeVersion.version_0 || encodeVersion == EncodeVersion.version_2) {
            int size = BytesUtils.toInt(keyMeta.getExtra());
            return IntegerReply.parse(size);
        } else if (encodeVersion == EncodeVersion.version_1) {
            KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());
            long size = getSizeFromKv(keyMeta, key);
            return IntegerReply.parse(size);
        } else if (encodeVersion == EncodeVersion.version_3) {
            reply = sync(cacheRedisTemplate.sendCommand(new Command(new byte[][]{RedisCommand.HLEN.raw(), cacheKey})));
            if (reply instanceof IntegerReply) {
                Long size = ((IntegerReply) reply).getInteger();
                if (size != null && size > 0) {
                    KvCacheMonitor.redisCache(cacheConfig.getNamespace(), redisCommand().strRaw());
                    return reply;
                }
            }
            KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());
            long size = getSizeFromKv(keyMeta, key);
            return IntegerReply.parse(size);
        } else {
            return ErrorReply.INTERNAL_ERROR;
        }
    }

    private Reply getSizeFromCache(KeyMeta keyMeta, byte[] key, byte[] cacheKey) {
        WriteBufferValue<RedisHash> writeBufferValue = hashWriteBuffer.get(cacheKey);
        if (writeBufferValue != null) {
            RedisHash hash = writeBufferValue.getValue();
            KvCacheMonitor.writeBuffer(cacheConfig.getNamespace(), redisCommand().strRaw());
            return IntegerReply.parse(hash.hlen());
        }
        if (cacheConfig.isHashLocalCacheEnable()) {
            HashLRUCache hashLRUCache = cacheConfig.getHashLRUCache();
            RedisHash hash = hashLRUCache.getForRead(key, cacheKey);
            if (hash != null) {
                KvCacheMonitor.localCache(cacheConfig.getNamespace(), redisCommand().strRaw());
                return IntegerReply.parse(hash.hlen());
            }
            EncodeVersion encodeVersion = keyMeta.getEncodeVersion();
            if (encodeVersion == EncodeVersion.version_1) {
                boolean hotKey = hashLRUCache.isHotKey(key);
                if (hotKey) {
                    hash = loadLRUCache(keyMeta, key);
                    hashLRUCache.putAllForRead(key, cacheKey, hash);
                    return IntegerReply.parse(hash.hlen());
                }
            }
        }
        return null;
    }

    private long getSizeFromKv(KeyMeta keyMeta, byte[] key) {
        byte[] startKey = keyDesign.hashFieldSubKey(keyMeta, key, new byte[0]);
        return kvClient.countByPrefix(startKey, startKey, false);
    }
}
