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
import com.netease.nim.camellia.redis.proxy.upstream.kv.domain.Index;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.KeyValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.EncodeVersion;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyType;
import com.netease.nim.camellia.redis.proxy.upstream.kv.utils.BytesUtils;
import com.netease.nim.camellia.tools.utils.BytesKey;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * ZREM key member [member ...]
 * <p>
 * Created by caojiajun on 2024/5/8
 */
public class ZRemCommander extends ZSet0Commander {

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

        Map<BytesKey, Double> localCacheResult = null;
        Result result = null;
        KvCacheMonitor.Type type = null;

        WriteBufferValue<RedisZSet> bufferValue = zsetWriteBuffer.get(cacheKey);
        if (bufferValue != null) {
            RedisZSet zSet = bufferValue.getValue();
            localCacheResult = zSet.zrem(members);
            //
            type = KvCacheMonitor.Type.write_buffer;
            KvCacheMonitor.writeBuffer(cacheConfig.getNamespace(), redisCommand().strRaw());
            if (localCacheResult.isEmpty()) {
                return IntegerReply.REPLY_0;
            }
            result = zsetWriteBuffer.put(cacheKey, zSet);
        }

        if (cacheConfig.isZSetLocalCacheEnable()) {
            ZSetLRUCache zSetLRUCache = cacheConfig.getZSetLRUCache();

            if (localCacheResult == null) {
                localCacheResult = zSetLRUCache.zrem(key, cacheKey, members);
                if (localCacheResult != null) {
                    type = KvCacheMonitor.Type.local_cache;
                    KvCacheMonitor.localCache(cacheConfig.getNamespace(), redisCommand().strRaw());
                }
            } else {
                zSetLRUCache.zrem(key, cacheKey, members);
            }

            if (localCacheResult == null) {
                boolean hotKey = zSetLRUCache.isHotKey(key);
                if (hotKey) {
                    RedisZSet zSet = loadLRUCache(keyMeta, key);
                    if (zSet != null) {
                        type = KvCacheMonitor.Type.kv_store;
                        KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());
                        //
                        zSetLRUCache.putZSetForWrite(key, cacheKey, zSet);
                        //
                        localCacheResult = zSet.zrem(members);

                        if (localCacheResult != null && localCacheResult.isEmpty()) {
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
            return zremVersion0(keyMeta, key, cacheKey, members, localCacheResult, result, type);
        }

        if (encodeVersion == EncodeVersion.version_1) {
            return zremVersion1(keyMeta, key, cacheKey, members, result, type);
        }

        return ErrorReply.INTERNAL_ERROR;
    }

    private Reply zremVersion0(KeyMeta keyMeta, byte[] key, byte[] cacheKey, Set<BytesKey> members, Map<BytesKey, Double> localCacheResult, Result result, KvCacheMonitor.Type type) {
        if (type != null) {
            KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());
        }

        if (localCacheResult != null) {
            List<byte[]> delStoreKeys = new ArrayList<>(localCacheResult.size() * 2);
            for (Map.Entry<BytesKey, Double> entry : localCacheResult.entrySet()) {
                byte[] zsetMemberSubKey1 = keyDesign.zsetMemberSubKey1(keyMeta, key, entry.getKey().getKey());
                byte[] zsetMemberSubKey2 = keyDesign.zsetMemberSubKey2(keyMeta, key, entry.getKey().getKey(), BytesUtils.toBytes(entry.getValue()));
                delStoreKeys.add(zsetMemberSubKey1);
                delStoreKeys.add(zsetMemberSubKey2);
            }

            if (result.isKvWriteDelayEnable()) {
                submitAsyncWriteTask(cacheKey, result, () -> kvClient.batchDelete(delStoreKeys.toArray(new byte[0][0])));
            } else {
                kvClient.batchDelete(delStoreKeys.toArray(new byte[0][0]));
            }

            int deleteCount = delStoreKeys.size() / 2;
            updateKeyMeta(keyMeta, key, deleteCount);
            return IntegerReply.parse(deleteCount);
        }

        byte[][] keys = new byte[members.size()][];
        int i=0;
        for (BytesKey storeKey : members) {
            keys[i] = keyDesign.zsetMemberSubKey1(keyMeta, key, storeKey.getKey());
            i ++;
        }
        List<KeyValue> keyValues = kvClient.batchGet(keys);
        List<byte[]> delStoreKeys = new ArrayList<>(members.size() * 2);
        for (KeyValue keyValue : keyValues) {
            if (keyValue == null || keyValue.getValue() == null) {
                continue;
            }
            byte[] member = keyDesign.decodeZSetMemberBySubKey1(keyValue.getKey(), key);
            byte[] score = keyValue.getValue();
            delStoreKeys.add(keyValue.getKey());
            delStoreKeys.add(keyDesign.zsetMemberSubKey2(keyMeta, key, member, score));
        }

        kvClient.batchDelete(delStoreKeys.toArray(new byte[0][0]));

        int deleteCount = delStoreKeys.size() / 2;
        updateKeyMeta(keyMeta, key, deleteCount);

        return IntegerReply.parse(deleteCount);
    }

    private Reply zremVersion1(KeyMeta keyMeta, byte[] key, byte[] cacheKey, Set<BytesKey> members, Result result, KvCacheMonitor.Type type) {
        if (type == null) {
            KvCacheMonitor.redisCache(cacheConfig.getNamespace(), redisCommand().strRaw());
        }

        byte[][] cmd = new byte[members.size() + 2][];
        cmd[0] = RedisCommand.ZREM.raw();
        cmd[1] = cacheKey;
        int i = 2;

        List<byte[]> indexCacheDeleteCmd = new ArrayList<>(members.size() + 1);
        indexCacheDeleteCmd.add(RedisCommand.DEL.raw());

        List<byte[]> deleteIndexKeys = new ArrayList<>(members.size());

        for (BytesKey member : members) {
            Index index = Index.fromRaw(member.getKey());
            if (index.isIndex()) {
                indexCacheDeleteCmd.add(keyDesign.zsetMemberIndexCacheKey(keyMeta, key, index));
                deleteIndexKeys.add(keyDesign.zsetIndexSubKey(keyMeta, key, index));
            }
            cmd[i] = index.getRef();
            i++;
        }

        List<Command> commandList = new ArrayList<>(3);
        commandList.add(new Command(cmd));
        commandList.add(new Command(new byte[][]{RedisCommand.ZCARD.raw(), cacheKey}));
        if (indexCacheDeleteCmd.size() > 1) {
            commandList.add(new Command(indexCacheDeleteCmd.toArray(new byte[0][0])));
        }

        List<CompletableFuture<Reply>> futures = redisTemplate.sendCommand(commandList);

        if (result.isKvWriteDelayEnable()) {
            submitAsyncWriteTask(cacheKey, result, () -> {
                if (!deleteIndexKeys.isEmpty()) {
                    kvClient.batchDelete(deleteIndexKeys.toArray(new byte[0][0]));
                }
            });
        } else {
            if (!deleteIndexKeys.isEmpty()) {
                kvClient.batchDelete(deleteIndexKeys.toArray(new byte[0][0]));
            }
        }

        List<Reply> replyList = sync(futures);
        for (Reply reply : replyList) {
            if (reply instanceof ErrorReply) {
                return reply;
            }
        }

        Reply reply = replyList.get(1);
        if (reply instanceof IntegerReply) {
            if (((IntegerReply) reply).getInteger() == 0) {
                keyMetaServer.deleteKeyMeta(key);
            }
        }
        return replyList.get(0);
    }

    private void updateKeyMeta(KeyMeta keyMeta, byte[] key, int deleteCount) {
        if (deleteCount > 0) {
            int count = BytesUtils.toInt(keyMeta.getExtra()) - deleteCount;
            if (count <= 0) {
                keyMetaServer.deleteKeyMeta(key);
                return;
            }
            byte[] extra = BytesUtils.toBytes(count);
            keyMeta = new KeyMeta(keyMeta.getEncodeVersion(), keyMeta.getKeyType(), keyMeta.getKeyVersion(), keyMeta.getExpireTime(), extra);
            keyMetaServer.createOrUpdateKeyMeta(key, keyMeta);
        }
    }
}
