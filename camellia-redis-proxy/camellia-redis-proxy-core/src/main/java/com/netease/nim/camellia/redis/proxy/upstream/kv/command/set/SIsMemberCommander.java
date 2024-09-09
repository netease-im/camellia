package com.netease.nim.camellia.redis.proxy.upstream.kv.command.set;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.monitor.KvCacheMonitor;
import com.netease.nim.camellia.redis.proxy.reply.*;
import com.netease.nim.camellia.redis.proxy.upstream.kv.buffer.WriteBufferValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.RedisSet;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.SetLRUCache;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.ValueWrapper;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.CommanderConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.KeyValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyType;
import com.netease.nim.camellia.tools.utils.BytesKey;

/**
 * SISMEMBER key member
 * <p>
 * Created by caojiajun on 2024/8/5
 */
public class SIsMemberCommander extends Set0Commander {

    public SIsMemberCommander(CommanderConfig commanderConfig) {
        super(commanderConfig);
    }

    @Override
    public RedisCommand redisCommand() {
        return RedisCommand.SISMEMBER;
    }

    @Override
    protected boolean parse(Command command) {
        return command.getObjects().length == 3;
    }

    @Override
    public Reply runToCompletion(int slot, Command command) {
        byte[][] objects = command.getObjects();
        byte[] key = objects[1];
        byte[] member = objects[2];
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

        byte[] cacheKey = keyDesign.cacheKey(keyMeta, key);

        WriteBufferValue<RedisSet> bufferValue = setWriteBuffer.get(cacheKey);
        if (bufferValue != null) {
            RedisSet set = bufferValue.getValue();
            KvCacheMonitor.writeBuffer(cacheConfig.getNamespace(), redisCommand().strRaw());
            boolean sismeber = set.sismeber(new BytesKey(member));
            return IntegerReply.parse(sismeber ? 1 : 0);
        }
        if (cacheConfig.isSetLocalCacheEnable()) {
            SetLRUCache setLRUCache = cacheConfig.getSetLRUCache();

            RedisSet set = setLRUCache.getForRead(slot, cacheKey);

            if (set != null) {
                KvCacheMonitor.localCache(cacheConfig.getNamespace(), redisCommand().strRaw());
                boolean sismeber = set.sismeber(new BytesKey(member));
                return IntegerReply.parse(sismeber ? 1 : 0);
            }
        }
        return null;
    }

    @Override
    protected Reply execute(int slot, Command command) {
        byte[][] objects = command.getObjects();
        byte[] key = objects[1];
        byte[] member = objects[2];
        KeyMeta keyMeta = keyMetaServer.getKeyMeta(slot, key);
        if (keyMeta == null) {
            return IntegerReply.REPLY_0;
        }
        if (keyMeta.getKeyType() != KeyType.set) {
            return ErrorReply.WRONG_TYPE;
        }

        byte[] cacheKey = keyDesign.cacheKey(keyMeta, key);

        WriteBufferValue<RedisSet> bufferValue = setWriteBuffer.get(cacheKey);
        if (bufferValue != null) {
            RedisSet set = bufferValue.getValue();
            KvCacheMonitor.writeBuffer(cacheConfig.getNamespace(), redisCommand().strRaw());
            boolean sismeber = set.sismeber(new BytesKey(member));
            return IntegerReply.parse(sismeber ? 1 : 0);
        }
        if (cacheConfig.isSetLocalCacheEnable()) {
            SetLRUCache setLRUCache = cacheConfig.getSetLRUCache();

            RedisSet set = setLRUCache.getForRead(slot, cacheKey);

            if (set != null) {
                KvCacheMonitor.localCache(cacheConfig.getNamespace(), redisCommand().strRaw());
                boolean sismeber = set.sismeber(new BytesKey(member));
                return IntegerReply.parse(sismeber ? 1 : 0);
            }

            boolean hotKey = setLRUCache.isHotKey(key);

            if (hotKey) {
                set = loadLRUCache(slot, keyMeta, key);
                setLRUCache.putAllForRead(slot, cacheKey, set);
                //
                KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());
                boolean sismeber = set.sismeber(new BytesKey(member));
                return IntegerReply.parse(sismeber ? 1 : 0);
            }
        }

        KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());

        byte[] subKey = keyDesign.setMemberSubKey(keyMeta, key, member);
        KeyValue keyValue = kvClient.get(slot, subKey);
        if (keyValue == null || keyValue.getKey() == null) {
            return IntegerReply.REPLY_0;
        }
        return IntegerReply.REPLY_1;
    }
}
