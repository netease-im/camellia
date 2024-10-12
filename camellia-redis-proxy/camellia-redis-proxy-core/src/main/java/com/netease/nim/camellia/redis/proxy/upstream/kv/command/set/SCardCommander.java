package com.netease.nim.camellia.redis.proxy.upstream.kv.command.set;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.monitor.KvCacheMonitor;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.IntegerReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.upstream.kv.buffer.WriteBufferValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.RedisSet;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.SetLRUCache;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.ValueWrapper;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.CommanderConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.EncodeVersion;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyType;
import com.netease.nim.camellia.redis.proxy.upstream.kv.utils.BytesUtils;

/**
 * SCARD key
 * <p>
 * Created by caojiajun on 2024/8/5
 */
public class SCardCommander extends Set0Commander {

    public SCardCommander(CommanderConfig commanderConfig) {
        super(commanderConfig);
    }

    @Override
    public RedisCommand redisCommand() {
        return RedisCommand.SCARD;
    }

    @Override
    protected boolean parse(Command command) {
        return command.getObjects().length == 2;
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
        if (keyMeta.getKeyType() != KeyType.set) {
            return ErrorReply.WRONG_TYPE;
        }

        EncodeVersion encodeVersion = keyMeta.getEncodeVersion();

        byte[] cacheKey = keyDesign.cacheKey(keyMeta, key);
        WriteBufferValue<RedisSet> bufferValue = setWriteBuffer.get(cacheKey);
        if (bufferValue != null) {
            RedisSet set = bufferValue.getValue();
            //
            KvCacheMonitor.writeBuffer(cacheConfig.getNamespace(), redisCommand().strRaw());
            return IntegerReply.parse(set.scard());
        }
        if (cacheConfig.isSetLocalCacheEnable()) {
            SetLRUCache setLRUCache = cacheConfig.getSetLRUCache();

            RedisSet set = setLRUCache.getForRead(slot, cacheKey);

            if (set != null) {
                KvCacheMonitor.localCache(cacheConfig.getNamespace(), redisCommand().strRaw());
                return IntegerReply.parse(set.scard());
            }
        }

        if (encodeVersion == EncodeVersion.version_0) {
            return IntegerReply.parse(BytesUtils.toInt(keyMeta.getExtra()));
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
        if (keyMeta.getKeyType() != KeyType.set) {
            return ErrorReply.WRONG_TYPE;
        }

        EncodeVersion encodeVersion = keyMeta.getEncodeVersion();

        byte[] cacheKey = keyDesign.cacheKey(keyMeta, key);
        WriteBufferValue<RedisSet> bufferValue = setWriteBuffer.get(cacheKey);
        if (bufferValue != null) {
            RedisSet set = bufferValue.getValue();
            //
            KvCacheMonitor.writeBuffer(cacheConfig.getNamespace(), redisCommand().strRaw());
            return IntegerReply.parse(set.scard());
        }
        if (cacheConfig.isSetLocalCacheEnable()) {
            SetLRUCache setLRUCache = cacheConfig.getSetLRUCache();

            RedisSet set = setLRUCache.getForRead(slot, cacheKey);

            if (set != null) {
                KvCacheMonitor.localCache(cacheConfig.getNamespace(), redisCommand().strRaw());
                return IntegerReply.parse(set.scard());
            }

            if (encodeVersion == EncodeVersion.version_1) {
                boolean hotKey = setLRUCache.isHotKey(key, redisCommand());
                if (hotKey) {
                    set = loadLRUCache(slot, keyMeta, key);
                    setLRUCache.putAllForRead(slot, cacheKey, set);
                    //
                    KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());
                    return IntegerReply.parse(set.scard());
                }
            }
        }

        if (encodeVersion == EncodeVersion.version_0) {
            return IntegerReply.parse(BytesUtils.toInt(keyMeta.getExtra()));
        }

        if (encodeVersion == EncodeVersion.version_1) {
            KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());
            long size = getSizeFromKv(slot, keyMeta, key);
            return IntegerReply.parse(size);
        }

        return ErrorReply.INTERNAL_ERROR;
    }

    private long getSizeFromKv(int slot, KeyMeta keyMeta, byte[] key) {
        byte[] startKey = keyDesign.setMemberSubKey(keyMeta, key, new byte[0]);
        return kvClient.countByPrefix(slot, startKey, startKey, false);
    }
}
