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
import com.netease.nim.camellia.redis.proxy.upstream.kv.domain.DeleteType;
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

        EncodeVersion encodeVersion = keyMeta.getEncodeVersion();

        Set<BytesKey> fields = new HashSet<>(objects.length - 2);
        for (int i = 2; i < objects.length; i++) {
            fields.add(new BytesKey(objects[i]));
        }

        byte[] cacheKey = keyDesign.cacheKey(keyMeta, key);

        KvCacheMonitor.Type type = null;

        int delCount = -1;

        Result result = null;
        DeleteType deleteType = DeleteType.unknown;

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
            if (hash.isEmpty()) {
                deleteType = DeleteType.delete_all;
            } else {
                deleteType = DeleteType.delete_someone;
            }
        }

        if (cacheConfig.isHashLocalCacheEnable()) {
            HashLRUCache hashLRUCache = cacheConfig.getHashLRUCache();

            Map<BytesKey, byte[]> deleteMaps = hashLRUCache.hdel(slot, cacheKey, fields);
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
                    RedisHash hash = loadLRUCache(slot, keyMeta, key);
                    hashLRUCache.putAllForWrite(slot, cacheKey, hash);
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
                RedisHash hash = hashLRUCache.getForWrite(slot, cacheKey);
                if (hash != null) {
                    result = hashWriteBuffer.put(cacheKey, hash.duplicate());
                    if (hash.isEmpty()) {
                        deleteType = DeleteType.delete_all;
                    } else {
                        deleteType = DeleteType.delete_someone;
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

        if (delCount < 0) {
            if (encodeVersion == EncodeVersion.version_0) {
                boolean[] exists = kvClient.exists(slot, subKeys);
                delCount = Utils.count(exists);
            }
        }

        if (encodeVersion == EncodeVersion.version_0) {
            batchDeleteSubKeys(slot, key, keyMeta, cacheKey, result, subKeys, false);
        } else {
            boolean checkHLen = deleteType == DeleteType.unknown;
            batchDeleteSubKeys(slot, key, keyMeta, cacheKey, result, subKeys, checkHLen);
        }

        if (deleteType == DeleteType.delete_all) {
            keyMetaServer.deleteKeyMeta(slot, key);
        } else {
            if (encodeVersion == EncodeVersion.version_0) {
                if (delCount > 0) {
                    int size = BytesUtils.toInt(keyMeta.getExtra()) - delCount;
                    if (size <= 0) {
                        keyMetaServer.deleteKeyMeta(slot, key);
                    } else {
                        byte[] extra = BytesUtils.toBytes(size);
                        keyMeta = new KeyMeta(keyMeta.getEncodeVersion(), keyMeta.getKeyType(), keyMeta.getKeyVersion(), keyMeta.getExpireTime(), extra);
                        keyMetaServer.createOrUpdateKeyMeta(slot, key, keyMeta);
                    }
                }
            }
        }

        if (encodeVersion == EncodeVersion.version_0) {
            return IntegerReply.parse(delCount);
        } else {
            return IntegerReply.parse(fieldSize);
        }
    }

    private void batchDeleteSubKeys(int slot, byte[] key, KeyMeta keyMeta, byte[] cacheKey, Result result, byte[][] subKeys, boolean checkHLen) {
        if (!result.isKvWriteDelayEnable()) {
            kvClient.batchDelete(slot, subKeys);
            if (checkHLen) {
                if (checkHLenZero(slot, key, keyMeta)) {
                    keyMetaServer.deleteKeyMeta(slot, key);
                }
            }
        } else {
            submitAsyncWriteTask(cacheKey, result, () -> {
                kvClient.batchDelete(slot, subKeys);
                if (checkHLen) {
                    if (checkHLenZero(slot, key, keyMeta)) {
                        keyMetaServer.deleteKeyMeta(slot, key);
                    }
                }
            });
        }
    }

    private boolean checkHLenZero(int slot, byte[] key, KeyMeta keyMeta) {
        byte[] startKey = keyDesign.hashFieldSubKey(keyMeta, key, new byte[0]);
        List<KeyValue> scan = kvClient.scanByPrefix(slot, startKey, startKey, 1, Sort.ASC, false);
        return scan.isEmpty();
    }
}
