package com.netease.nim.camellia.redis.proxy.upstream.kv.command.hash;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.monitor.KvCacheMonitor;
import com.netease.nim.camellia.redis.proxy.upstream.kv.buffer.NoOpResult;
import com.netease.nim.camellia.redis.proxy.upstream.kv.buffer.Result;
import com.netease.nim.camellia.redis.proxy.upstream.kv.buffer.WriteBufferValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.RedisHash;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.HashLRUCache;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.CommanderConfig;
import com.netease.nim.camellia.redis.proxy.reply.*;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.KeyValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.Sort;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.EncodeVersion;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyType;
import com.netease.nim.camellia.redis.proxy.upstream.kv.utils.BytesUtils;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import com.netease.nim.camellia.tools.utils.BytesKey;

import java.util.*;

/**
 * HDEL key field [field ...]
 * <p>
 * Created by caojiajun on 2024/4/11
 */
public class HDelCommander extends Hash0Commander {

    public HDelCommander(CommanderConfig commanderConfig) {
        super(commanderConfig);
    }

    @Override
    public RedisCommand redisCommand() {
        return RedisCommand.HDEL;
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
            return IntegerReply.REPLY_0;
        }
        if (keyMeta.getKeyType() != KeyType.hash) {
            return ErrorReply.WRONG_TYPE;
        }

        EncodeVersion encodeVersion = keyMeta.getEncodeVersion();

        Set<BytesKey> fields = new HashSet<>(objects.length - 2);
        for (int i = 2; i < objects.length; i++) {
            fields.add(new BytesKey(objects[i]));
        }

        byte[] cacheKey = keyDesign.cacheKey(keyMeta, key);

        KvCacheMonitor.Type type = null;

        int delCount = -1;

        Result result = null;
        boolean deleteAll = false;
        WriteBufferValue<RedisHash> writeBufferValue = hashWriteBuffer.get(cacheKey);
        if (writeBufferValue != null) {
            type = KvCacheMonitor.Type.write_buffer;
            KvCacheMonitor.writeBuffer(cacheConfig.getNamespace(), redisCommand().strRaw());
            RedisHash hash = writeBufferValue.getValue();
            Map<BytesKey, byte[]> deleteMaps = hash.hdel(fields);
            delCount = deleteMaps.size();
            if (delCount == 0) {
                if (encodeVersion == EncodeVersion.version_1) {
                    return IntegerReply.parse(fields.size());
                }
                return IntegerReply.REPLY_0;
            }
            result = hashWriteBuffer.put(cacheKey, hash);
            deleteAll = hash.isEmpty();
        }

        if (cacheConfig.isHashLocalCacheEnable()) {
            HashLRUCache hashLRUCache = cacheConfig.getHashLRUCache();

            Map<BytesKey, byte[]> deleteMaps = hashLRUCache.hdel(key, cacheKey, fields);
            if (deleteMaps != null) {
                type = KvCacheMonitor.Type.write_buffer;
                KvCacheMonitor.localCache(cacheConfig.getNamespace(), redisCommand().strRaw());
            }
            if (deleteMaps != null && delCount < 0) {
                delCount = deleteMaps.size();
            }
            if (delCount == 0) {
                if (encodeVersion == EncodeVersion.version_1) {
                    return IntegerReply.parse(fields.size());
                }
                return IntegerReply.REPLY_0;
            }
            if (deleteMaps == null) {
                boolean hotKey = hashLRUCache.isHotKey(key);
                if (hotKey) {
                    //
                    type = KvCacheMonitor.Type.kv_store;
                    KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());
                    //
                    RedisHash hash = loadLRUCache(keyMeta, key);
                    hashLRUCache.putAllForWrite(key, cacheKey, hash);
                    deleteMaps = hash.hdel(fields);
                    if (deleteMaps != null && delCount < 0) {
                        delCount = deleteMaps.size();
                    }
                    if (delCount == 0) {
                        if (encodeVersion == EncodeVersion.version_1) {
                            return IntegerReply.parse(fields.size());
                        }
                        return IntegerReply.REPLY_0;
                    }
                }
            }
            if (deleteMaps != null && result == null) {
                RedisHash hash = hashLRUCache.getForWrite(key, cacheKey);
                if (hash != null) {
                    result = hashWriteBuffer.put(cacheKey, hash.duplicate());
                    if (hash.isEmpty()) {
                        deleteAll = true;
                    }
                }
            }
        }

        if (result == null) {
            result = NoOpResult.INSTANCE;
        }

        int fieldSize = fields.size();

        if (type == null) {
            KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());
        }
        byte[][] subKeys = new byte[fieldSize][];
        int i=0;
        for (BytesKey field : fields) {
            subKeys[i] = keyDesign.hashFieldSubKey(keyMeta, key, field.getKey());
            i++;
        }
        if (encodeVersion == EncodeVersion.version_0) {
            if (delCount < 0) {
                boolean[] exists = kvClient.exists(subKeys);
                delCount = Utils.count(exists);
            }
            if (delCount > 0) {
                int size = BytesUtils.toInt(keyMeta.getExtra()) - delCount;
                if (size <= 0 || deleteAll) {
                    keyMetaServer.deleteKeyMeta(key);
                } else {
                    byte[] extra = BytesUtils.toBytes(size);
                    keyMeta = new KeyMeta(keyMeta.getEncodeVersion(), keyMeta.getKeyType(), keyMeta.getKeyVersion(), keyMeta.getExpireTime(), extra);
                    keyMetaServer.createOrUpdateKeyMeta(key, keyMeta);
                }
            }
            batchDeleteSubKeys(key, keyMeta, cacheKey, result, subKeys, false);
            return IntegerReply.parse(delCount);
        } else {
            if (deleteAll) {
                keyMetaServer.deleteKeyMeta(key);
            }
            batchDeleteSubKeys(key, keyMeta, cacheKey, result, subKeys, !deleteAll);
            return IntegerReply.parse(fieldSize);
        }
    }

    private void batchDeleteSubKeys(byte[] key, KeyMeta keyMeta, byte[] cacheKey, Result result, byte[][] subKeys, boolean checkHLen) {
        if (!result.isKvWriteDelayEnable()) {
            kvClient.batchDelete(subKeys);
            if (checkHLen) {
                if (checkHLenZero(key, keyMeta)) {
                    keyMetaServer.deleteKeyMeta(key);
                }
            }
        } else {
            submitAsyncWriteTask(cacheKey, result, () -> {
                kvClient.batchDelete(subKeys);
                if (checkHLen) {
                    if (checkHLenZero(key, keyMeta)) {
                        keyMetaServer.deleteKeyMeta(key);
                    }
                }
            });
        }
    }

    private boolean checkHLenZero(byte[] key, KeyMeta keyMeta) {
        byte[] startKey = keyDesign.hashFieldSubKey(keyMeta, key, new byte[0]);
        List<KeyValue> scan = kvClient.scanByPrefix(startKey, startKey, 1, Sort.ASC, false);
        return scan.isEmpty();
    }
}
