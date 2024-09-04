package com.netease.nim.camellia.redis.proxy.upstream.kv.command.hash;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.monitor.KvCacheMonitor;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.IntegerReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.upstream.kv.buffer.NoOpResult;
import com.netease.nim.camellia.redis.proxy.upstream.kv.buffer.Result;
import com.netease.nim.camellia.redis.proxy.upstream.kv.buffer.WriteBufferValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.RedisHash;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.HashLRUCache;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.CommanderConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.KeyValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.EncodeVersion;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyType;
import com.netease.nim.camellia.redis.proxy.upstream.kv.utils.BytesUtils;
import com.netease.nim.camellia.tools.utils.BytesKey;

import java.util.HashMap;
import java.util.Map;

/**
 * HSETNX key field value
 * <p>
 * Created by caojiajun on 2024/6/6
 */
public class HSetNxCommander extends Hash0Commander {

    private static final int cache_miss = 0;
    private static final int cache_hit_not_exists = 2;

    public HSetNxCommander(CommanderConfig commanderConfig) {
        super(commanderConfig);
    }

    @Override
    public RedisCommand redisCommand() {
        return RedisCommand.HSETNX;
    }

    @Override
    protected boolean parse(Command command) {
        byte[][] objects = command.getObjects();
        return objects.length == 4;
    }

    @Override
    protected Reply execute(int slot, Command command) {
        byte[][] objects = command.getObjects();
        byte[] key = objects[1];
        byte[] field = objects[2];
        byte[] value = objects[3];

        boolean first = false;

        //check meta
        KeyMeta keyMeta = keyMetaServer.getKeyMeta(slot, key);
        if (keyMeta == null) {
            EncodeVersion encodeVersion = keyDesign.hashEncodeVersion();
            if (encodeVersion == EncodeVersion.version_0) {
                byte[] extra = BytesUtils.toBytes(1);
                keyMeta = new KeyMeta(encodeVersion, KeyType.hash, System.currentTimeMillis(), -1, extra);
            } else if (encodeVersion == EncodeVersion.version_1) {
                keyMeta = new KeyMeta(encodeVersion, KeyType.hash, System.currentTimeMillis(), -1);
            } else {
                return ErrorReply.INTERNAL_ERROR;
            }
            keyMetaServer.createOrUpdateKeyMeta(slot, key, keyMeta);
            first = true;
        } else {
            if (keyMeta.getKeyType() != KeyType.hash) {
                return ErrorReply.WRONG_TYPE;
            }
        }


        byte[] cacheKey = keyDesign.cacheKey(keyMeta, key);
        BytesKey filedKey = new BytesKey(field);

        if (first) {
            Map<BytesKey, byte[]> fieldMap = new HashMap<>();
            fieldMap.put(new BytesKey(field), value);
            Result result = hashWriteBuffer.put(cacheKey, new RedisHash(fieldMap));

            if (result == NoOpResult.INSTANCE) {
                KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());
            } else {
                KvCacheMonitor.writeBuffer(cacheConfig.getNamespace(), redisCommand().strRaw());
            }

            if (cacheConfig.isHashLocalCacheEnable()) {
                HashLRUCache hashLRUCache = cacheConfig.getHashLRUCache();
                hashLRUCache.putAllForWrite(slot, cacheKey, new RedisHash(new HashMap<>(fieldMap)));
            }

            byte[] subKey = keyDesign.hashFieldSubKey(keyMeta, key, field);

            if (result.isKvWriteDelayEnable()) {
                submitAsyncWriteTask(cacheKey, result, () -> kvClient.put(slot, subKey, value));
            } else {
                kvClient.put(slot, subKey, value);
            }
            return IntegerReply.REPLY_1;
        }

        int cacheCheck = cache_miss;

        KvCacheMonitor.Type type = null;

        Result result = null;
        WriteBufferValue<RedisHash> writeBufferValue = hashWriteBuffer.get(cacheKey);
        if (writeBufferValue != null) {
            //
            type = KvCacheMonitor.Type.write_buffer;
            KvCacheMonitor.writeBuffer(cacheConfig.getNamespace(), redisCommand().strRaw());
            //
            RedisHash hash = writeBufferValue.getValue();
            byte[] hget = hash.hget(filedKey);
            if (hget != null) {
                return IntegerReply.REPLY_0;
            } else {
                cacheCheck = cache_hit_not_exists;
            }
            hash.hset(filedKey, value);
            result = hashWriteBuffer.put(cacheKey, hash);
        }

        if (cacheConfig.isHashLocalCacheEnable()) {
            HashLRUCache hashLRUCache = cacheConfig.getHashLRUCache();

            RedisHash hash = hashLRUCache.getForWrite(slot, cacheKey);
            if (hash == null) {
                boolean hotKey = hashLRUCache.isHotKey(key);
                if (hotKey) {
                    //
                    type = KvCacheMonitor.Type.kv_store;
                    KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());
                    //
                    Map<BytesKey, byte[]> map = hgetallFromKv(slot, keyMeta, key);
                    hash = new RedisHash(map);
                    hashLRUCache.putAllForWrite(slot, cacheKey, hash);
                }
            } else {
                type = KvCacheMonitor.Type.local_cache;
                KvCacheMonitor.localCache(cacheConfig.getNamespace(), redisCommand().strRaw());
            }
            if (hash != null) {
                byte[] hget = hash.hget(filedKey);
                if (hget != null) {
                    return IntegerReply.REPLY_0;
                } else {
                    Map<BytesKey, byte[]> fieldMap = new HashMap<>();
                    fieldMap.put(filedKey, value);
                    hashLRUCache.hset(slot, cacheKey, fieldMap);
                    cacheCheck = cache_hit_not_exists;
                }
            }
            if (result == null) {
                hash = hashLRUCache.getForWrite(slot, cacheKey);
                if (hash != null) {
                    result = hashWriteBuffer.put(cacheKey, hash.duplicate());
                }
            }
        }

        if (result == null) {
            result = NoOpResult.INSTANCE;
        }

        if (type == null) {
            KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());
        }

        EncodeVersion encodeVersion = keyMeta.getEncodeVersion();

        KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());
        byte[] subKey = keyDesign.hashFieldSubKey(keyMeta, key, field);

        if (cacheCheck == cache_miss) {
            KeyValue keyValue = kvClient.get(slot, subKey);
            if (keyValue != null && keyValue.getValue() != null) {
                return IntegerReply.REPLY_0;
            }
        }

        if (encodeVersion == EncodeVersion.version_0) {
            int size = BytesUtils.toInt(keyMeta.getExtra());
            keyMeta = new KeyMeta(keyMeta.getEncodeVersion(), keyMeta.getKeyType(),
                    keyMeta.getKeyVersion(), keyMeta.getExpireTime(), BytesUtils.toBytes(size + 1));
            keyMetaServer.createOrUpdateKeyMeta(slot, key, keyMeta);
        }

        put(slot, cacheKey, result, new KeyValue(subKey, value));

        return IntegerReply.REPLY_1;
    }

    private void put(int slot, byte[] cacheKey, Result result, KeyValue keyValue) {
        if (!result.isKvWriteDelayEnable()) {
            kvClient.put(slot, keyValue.getKey(), keyValue.getValue());
        } else {
            submitAsyncWriteTask(cacheKey, result, () -> kvClient.put(slot, keyValue.getKey(), keyValue.getValue()));
        }
    }
}
