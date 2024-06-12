package com.netease.nim.camellia.redis.proxy.upstream.kv.command.zset;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.monitor.KvCacheMonitor;
import com.netease.nim.camellia.redis.proxy.reply.*;
import com.netease.nim.camellia.redis.proxy.upstream.kv.buffer.NoOpResult;
import com.netease.nim.camellia.redis.proxy.upstream.kv.buffer.Result;
import com.netease.nim.camellia.redis.proxy.upstream.kv.buffer.WriteBufferValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.ZSet;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.ZSetLRUCache;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.CommanderConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.domain.Index;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.KeyValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.EncodeVersion;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyType;
import com.netease.nim.camellia.redis.proxy.upstream.kv.utils.BytesUtils;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import com.netease.nim.camellia.tools.utils.BytesKey;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * ZREM key member [member ...]
 * <p>
 * Created by caojiajun on 2024/5/8
 */
public class ZRemCommander extends ZSet0Commander {

    private static final byte[] script = ("local ret1 = redis.call('exists', KEYS[1]);\n" +
            "if tonumber(ret1) == 1 then\n" +
            "  local ret = redis.call('zrem', KEYS[1], unpack(ARGV));\n" +
            "  return {'1', ret};\n" +
            "end\n" +
            "return {'2'};").getBytes(StandardCharsets.UTF_8);

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

        WriteBufferValue<ZSet> bufferValue = zsetWriteBuffer.get(cacheKey);
        if (bufferValue != null) {
            ZSet zSet = bufferValue.getValue();
            localCacheResult = zSet.zrem(members);
            KvCacheMonitor.writeBuffer(cacheConfig.getNamespace(), redisCommand().strRaw());
            if (localCacheResult.isEmpty()) {
                return IntegerReply.REPLY_0;
            }
            result = zsetWriteBuffer.put(cacheKey, zSet);
        }

        if (cacheConfig.isZSetLocalCacheEnable()) {
            ZSetLRUCache zSetLRUCache = cacheConfig.getZSetLRUCache();

            boolean hotKey = zSetLRUCache.isHotKey(key);

            if (localCacheResult == null) {
                localCacheResult = zSetLRUCache.zrem(key, cacheKey, members);
                if (localCacheResult != null) {
                    KvCacheMonitor.localCache(cacheConfig.getNamespace(), redisCommand().strRaw());
                }
            } else {
                zSetLRUCache.zrem(key, cacheKey, members);
            }

            if (hotKey && localCacheResult == null) {
                ZSet zSet = loadLRUCache(keyMeta, key);
                if (zSet != null) {
                    //
                    zSetLRUCache.putZSetForWrite(key, cacheKey, zSet);
                    //
                    localCacheResult = zSet.zrem(members);

                    if (localCacheResult != null && localCacheResult.isEmpty()) {
                        return IntegerReply.REPLY_0;
                    }
                }
            }

            if (result == null) {
                ZSet zSet = zSetLRUCache.getForWrite(key, cacheKey);
                if (zSet != null) {
                    result = zsetWriteBuffer.put(cacheKey, zSet.duplicate());
                }
            }
        }

        if (result == null) {
            result = NoOpResult.INSTANCE;
        }

        if (localCacheResult != null) {
            KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());
        }

        EncodeVersion encodeVersion = keyMeta.getEncodeVersion();
        if (encodeVersion == EncodeVersion.version_0) {
            return zremVersion0(keyMeta, key, cacheKey, members, localCacheResult, result);
        }
        if (encodeVersion == EncodeVersion.version_1) {
            return zremVersion1(keyMeta, key, cacheKey, members, localCacheResult, result);
        }
        if (encodeVersion == EncodeVersion.version_2) {
            return zremVersion2(keyMeta, key, cacheKey, members, localCacheResult, result);
        }
        if (encodeVersion == EncodeVersion.version_3) {
            return zremVersion3(keyMeta, key, cacheKey, members, result);
        }
        return ErrorReply.INTERNAL_ERROR;
    }

    private Reply zremVersion0(KeyMeta keyMeta, byte[] key, byte[] cacheKey, Set<BytesKey> members, Map<BytesKey, Double> localCacheResult, Result result) {
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

    private Reply zremVersion1(KeyMeta keyMeta, byte[] key, byte[] cacheKey, Set<BytesKey> members, Map<BytesKey, Double> localCacheResult, Result result) {
        byte[][] args = new byte[members.size()][];
        int i = 0;
        for (BytesKey member : members) {
            args[i] = member.getKey();
            i++;
        }
        Reply reply = sync(cacheRedisTemplate.sendLua(script, new byte[][]{cacheKey}, args));

        int deleteCount = -1;

        if (localCacheResult != null) {
            deleteCount = localCacheResult.size();
        }

        if (deleteCount < 0) {
            if (reply instanceof MultiBulkReply) {
                Reply[] replies = ((MultiBulkReply) reply).getReplies();
                if (replies[0] instanceof BulkReply) {
                    byte[] raw = ((BulkReply) replies[0]).getRaw();
                    if (Utils.bytesToString(raw).equalsIgnoreCase("1")) {
                        if (replies[1] instanceof IntegerReply) {
                            deleteCount = (((IntegerReply) replies[1]).getInteger()).intValue();
                            KvCacheMonitor.redisCache(cacheConfig.getNamespace(), redisCommand().strRaw());
                        }
                    }
                }
            }
        }

        byte[][] storeKeys = new byte[members.size()][];
        int j=0;
        for (BytesKey member : members) {
            storeKeys[j] = keyDesign.zsetMemberSubKey1(keyMeta, key, member.getKey());
            j++;
        }

        if (deleteCount < 0) {
            List<byte[]> existsStoreKeys = new ArrayList<>(args.length);
            List<KeyValue> keyValues = kvClient.batchGet(storeKeys);
            for (KeyValue keyValue : keyValues) {
                if (keyValue == null || keyValue.getValue() == null) {
                    continue;
                }
                existsStoreKeys.add(keyValue.getKey());
            }
            deleteCount = existsStoreKeys.size();
            kvClient.batchDelete(existsStoreKeys.toArray(new byte[0][0]));
        } else {
            if (result.isKvWriteDelayEnable()) {
                submitAsyncWriteTask(cacheKey, result, () -> kvClient.batchDelete(storeKeys));
            } else {
                kvClient.batchDelete(storeKeys);
            }
        }

        updateKeyMeta(keyMeta, key, deleteCount);

        if (reply instanceof ErrorReply) {
            return reply;
        }
        return IntegerReply.parse(deleteCount);
    }

    private Reply zremVersion2(KeyMeta keyMeta, byte[] key, byte[] cacheKey, Set<BytesKey> members, Map<BytesKey, Double> localCacheResult, Result result) {
        byte[][] args = new byte[members.size()][];

        List<byte[]> indexCacheDeleteCmd = new ArrayList<>(members.size() + 1);
        indexCacheDeleteCmd.add(RedisCommand.DEL.raw());

        List<byte[]> deleteStoreKeys = new ArrayList<>(members.size());
        List<byte[]> subKeys = new ArrayList<>(members.size());
        int i=0;
        for (BytesKey member : members) {
            Index index = Index.fromRaw(member.getKey());
            if (index.isIndex()) {
                indexCacheDeleteCmd.add(keyDesign.zsetMemberIndexCacheKey(keyMeta, key, index));
                deleteStoreKeys.add(keyDesign.zsetIndexSubKey(keyMeta, key, index));
            }
            args[i] = index.getRef();

            byte[] zsetMemberSubKey1 = keyDesign.zsetMemberSubKey1(keyMeta, key, member.getKey());
            subKeys.add(zsetMemberSubKey1);
            deleteStoreKeys.add(zsetMemberSubKey1);
            i++;
        }

        List<Command> commandList = new ArrayList<>(2);
        commandList.add(cacheRedisTemplate.luaCommand(script, new byte[][]{cacheKey}, args));
        if (indexCacheDeleteCmd.size() > 1) {
            commandList.add(new Command(indexCacheDeleteCmd.toArray(new byte[0][0])));
        }

        List<Reply> replyList = sync(cacheRedisTemplate.sendCommand(commandList));
        Reply reply = replyList.get(0);
        int deleteCount = -1;
        if (localCacheResult != null) {
            deleteCount = localCacheResult.size();
        }

        if (deleteCount < 0) {
            if (reply instanceof MultiBulkReply) {
                Reply[] replies = ((MultiBulkReply) reply).getReplies();
                if (replies[0] instanceof BulkReply) {
                    byte[] raw = ((BulkReply) replies[0]).getRaw();
                    if (Utils.bytesToString(raw).equalsIgnoreCase("1")) {
                        if (replies[1] instanceof IntegerReply) {
                            deleteCount = (((IntegerReply) replies[1]).getInteger()).intValue();
                            KvCacheMonitor.redisCache(cacheConfig.getNamespace(), redisCommand().strRaw());
                        }
                    }
                }
            }
        }

        if (deleteCount < 0) {
            deleteCount = 0;
            List<KeyValue> keyValues = kvClient.batchGet(subKeys.toArray(new byte[0][0]));
            for (KeyValue keyValue : keyValues) {
                if (keyValue == null || keyValue.getValue() == null) {
                    continue;
                }
                deleteCount ++;
            }
        }

        if (result.isKvWriteDelayEnable()) {
            submitAsyncWriteTask(cacheKey, result, () -> kvClient.batchDelete(deleteStoreKeys.toArray(new byte[0][0])));
        } else {
            kvClient.batchDelete(deleteStoreKeys.toArray(new byte[0][0]));
        }

        updateKeyMeta(keyMeta, key, deleteCount);

        for (Reply reply1 : replyList) {
            if (reply1 instanceof ErrorReply) {
                return reply1;
            }
        }
        return IntegerReply.parse(deleteCount);
    }

    private Reply zremVersion3(KeyMeta keyMeta, byte[] key, byte[] cacheKey, Set<BytesKey> members, Result result) {
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

        List<CompletableFuture<Reply>> futures = cacheRedisTemplate.sendCommand(commandList);

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
