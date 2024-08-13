package com.netease.nim.camellia.redis.proxy.upstream.kv.command.zset;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.monitor.KvCacheMonitor;
import com.netease.nim.camellia.redis.proxy.reply.*;
import com.netease.nim.camellia.redis.proxy.upstream.kv.buffer.NoOpResult;
import com.netease.nim.camellia.redis.proxy.upstream.kv.buffer.Result;
import com.netease.nim.camellia.redis.proxy.upstream.kv.buffer.WriteBufferValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.RedisZSet;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.ZSetLRUCache;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.CommanderConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.KeyValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.EncodeVersion;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyType;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import com.netease.nim.camellia.tools.utils.BytesKey;

import java.util.*;

/**
 * ZREM key member [member ...]
 * <p>
 * Created by caojiajun on 2024/5/8
 */
public class ZRemCommander extends ZRem0Commander {

    public ZRemCommander(CommanderConfig commanderConfig) {
        super(commanderConfig);
    }

    @Override
    public RedisCommand redisCommand() {
        return RedisCommand.ZREM;
    }

    @Override
    protected boolean parse(Command command) {
        byte[][] objects = command.getObjects();
        return objects.length >= 3;
    }

    @Override
    protected Reply execute(Command command) {
        byte[][] objects = command.getObjects();
        byte[] key = objects[1];
        KeyMeta keyMeta = keyMetaServer.getKeyMeta(key);
        if (keyMeta == null) {
            return MultiBulkReply.EMPTY;
        }
        if (keyMeta.getKeyType() != KeyType.zset) {
            return ErrorReply.WRONG_TYPE;
        }

        Set<BytesKey> members = new HashSet<>();
        for (int i=2; i<objects.length; i++) {
            members.add(new BytesKey(objects[i]));
        }

        byte[] cacheKey = keyDesign.cacheKey(keyMeta, key);

        Map<BytesKey, Double> removedMembers = null;
        Result result = null;
        KvCacheMonitor.Type type = null;

        WriteBufferValue<RedisZSet> bufferValue = zsetWriteBuffer.get(cacheKey);
        if (bufferValue != null) {
            RedisZSet zSet = bufferValue.getValue();
            removedMembers = zSet.zrem(members);
            //
            type = KvCacheMonitor.Type.write_buffer;
            KvCacheMonitor.writeBuffer(cacheConfig.getNamespace(), redisCommand().strRaw());
            if (removedMembers.isEmpty()) {
                return IntegerReply.REPLY_0;
            }
            result = zsetWriteBuffer.put(cacheKey, zSet);
        }

        if (cacheConfig.isZSetLocalCacheEnable()) {
            ZSetLRUCache zSetLRUCache = cacheConfig.getZSetLRUCache();

            if (removedMembers == null) {
                removedMembers = zSetLRUCache.zrem(key, cacheKey, members);
                if (removedMembers != null) {
                    type = KvCacheMonitor.Type.local_cache;
                    KvCacheMonitor.localCache(cacheConfig.getNamespace(), redisCommand().strRaw());
                }
            } else {
                zSetLRUCache.zrem(key, cacheKey, members);
            }

            if (removedMembers == null) {
                boolean hotKey = zSetLRUCache.isHotKey(key);
                if (hotKey) {
                    RedisZSet zSet = loadLRUCache(keyMeta, key);
                    if (zSet != null) {
                        type = KvCacheMonitor.Type.kv_store;
                        KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());
                        //
                        zSetLRUCache.putZSetForWrite(key, cacheKey, zSet);
                        //
                        removedMembers = zSet.zrem(members);

                        if (removedMembers != null && removedMembers.isEmpty()) {
                            return IntegerReply.REPLY_0;
                        }
                    }
                }
            }

            if (result == null) {
                RedisZSet zSet = zSetLRUCache.getForWrite(key, cacheKey);
                if (zSet != null) {
                    result = zsetWriteBuffer.put(cacheKey, zSet.duplicate());
                }
            }
        }

        if (result == null) {
            result = NoOpResult.INSTANCE;
        }

        EncodeVersion encodeVersion = keyMeta.getEncodeVersion();

        if (encodeVersion == EncodeVersion.version_0) {
            return zremVersion0(keyMeta, key, cacheKey, members, removedMembers, result, type);
        }

        if (encodeVersion == EncodeVersion.version_1) {
            return zremVersion1(keyMeta, key, cacheKey, members, result);
        }

        return ErrorReply.INTERNAL_ERROR;
    }

    private Reply zremVersion0(KeyMeta keyMeta, byte[] key, byte[] cacheKey, Set<BytesKey> members, Map<BytesKey, Double> removedMembers,
                               Result result, KvCacheMonitor.Type type) {
        if (type != null) {
            KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());
        }

        if (removedMembers == null) {
            removedMembers = new HashMap<>();

            byte[][] keys = new byte[members.size()][];
            int i = 0;
            for (BytesKey storeKey : members) {
                keys[i] = keyDesign.zsetMemberSubKey1(keyMeta, key, storeKey.getKey());
                i++;
            }
            List<KeyValue> keyValues = kvClient.batchGet(keys);
            for (KeyValue keyValue : keyValues) {
                if (keyValue == null || keyValue.getValue() == null) {
                    continue;
                }
                byte[] member = keyDesign.decodeZSetMemberBySubKey1(keyValue.getKey(), key);
                byte[] score = keyValue.getValue();
                removedMembers.put(new BytesKey(member), Utils.bytesToDouble(score));
            }
        }

        return zremVersion0(keyMeta, key, cacheKey, removedMembers, result);
    }

}
