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
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.ValueWrapper;
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
    public Reply runToCompletion(int slot, Command command) {
        byte[][] objects = command.getObjects();
        byte[] key = objects[1];
        ValueWrapper<KeyMeta> valueWrapper = keyMetaServer.runToComplete(slot, key);
        if (valueWrapper == null) {
            return null;
        }
        KeyMeta keyMeta = valueWrapper.get();
        if (keyMeta == null) {
            return IntegerReply.REPLY_0;
        }
        if (keyMeta.getKeyType() != KeyType.hash) {
            return ErrorReply.WRONG_TYPE;
        }

        byte[] cacheKey = keyDesign.cacheKey(keyMeta, key);

        Reply reply = getSizeFromCache(slot, keyMeta, key, cacheKey, false);
        if (reply != null) {
            return reply;
        }
        EncodeVersion encodeVersion = keyMeta.getEncodeVersion();
        if (encodeVersion == EncodeVersion.version_0) {
            int size = BytesUtils.toInt(keyMeta.getExtra());
            return IntegerReply.parse(size);
        }
        return null;
    }

    @Override
    protected Reply execute(int slot, Command command) {
        byte[][] objects = command.getObjects();
        byte[] key = objects[1];
        KeyMeta keyMeta = keyMetaServer.getKeyMeta(slot, key);
        if (keyMeta == null) {
            return IntegerReply.REPLY_0;
        }
        if (keyMeta.getKeyType() != KeyType.hash) {
            return ErrorReply.WRONG_TYPE;
        }

        byte[] cacheKey = keyDesign.cacheKey(keyMeta, key);

        Reply reply = getSizeFromCache(slot, keyMeta, key, cacheKey, true);
        if (reply != null) {
            return reply;
        }
        EncodeVersion encodeVersion = keyMeta.getEncodeVersion();
        if (encodeVersion == EncodeVersion.version_0) {
            int size = BytesUtils.toInt(keyMeta.getExtra());
            return IntegerReply.parse(size);
        } else if (encodeVersion == EncodeVersion.version_1) {
            KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());
            long size = getSizeFromKv(slot, keyMeta, key);
            return IntegerReply.parse(size);
        } else {
            return ErrorReply.INTERNAL_ERROR;
        }
    }

    private Reply getSizeFromCache(int slot, KeyMeta keyMeta, byte[] key, byte[] cacheKey, boolean checkHotKey) {
        WriteBufferValue<RedisHash> writeBufferValue = hashWriteBuffer.get(cacheKey);
        if (writeBufferValue != null) {
            RedisHash hash = writeBufferValue.getValue();
            KvCacheMonitor.writeBuffer(cacheConfig.getNamespace(), redisCommand().strRaw());
            return IntegerReply.parse(hash.hlen());
        }
        if (cacheConfig.isHashLocalCacheEnable()) {
            HashLRUCache hashLRUCache = cacheConfig.getHashLRUCache();
            RedisHash hash = hashLRUCache.getForRead(slot, cacheKey);
            if (hash != null) {
                KvCacheMonitor.localCache(cacheConfig.getNamespace(), redisCommand().strRaw());
                return IntegerReply.parse(hash.hlen());
            }
            if (checkHotKey) {
                EncodeVersion encodeVersion = keyMeta.getEncodeVersion();
                if (encodeVersion == EncodeVersion.version_1) {
                    boolean hotKey = hashLRUCache.isHotKey(key, redisCommand());
                    if (hotKey) {
                        hash = loadLRUCache(slot, keyMeta, key);
                        hashLRUCache.putAllForRead(slot, cacheKey, hash);
                        KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());
                        return IntegerReply.parse(hash.hlen());
                    }
                }
            }
        }
        return null;
    }

    private long getSizeFromKv(int slot, KeyMeta keyMeta, byte[] key) {
        byte[] startKey = keyDesign.hashFieldSubKey(keyMeta, key, new byte[0]);
        return kvClient.countByPrefix(slot, startKey, startKey, false);
    }
}
