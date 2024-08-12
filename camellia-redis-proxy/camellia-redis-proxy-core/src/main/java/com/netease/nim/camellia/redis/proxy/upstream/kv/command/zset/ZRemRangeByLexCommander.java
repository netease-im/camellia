package com.netease.nim.camellia.redis.proxy.upstream.kv.command.zset;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.monitor.KvCacheMonitor;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.IntegerReply;
import com.netease.nim.camellia.redis.proxy.reply.MultiBulkReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.upstream.kv.buffer.NoOpResult;
import com.netease.nim.camellia.redis.proxy.upstream.kv.buffer.Result;
import com.netease.nim.camellia.redis.proxy.upstream.kv.buffer.WriteBufferValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.RedisZSet;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.ZSetLRUCache;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.CommanderConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.domain.Index;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.KeyValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.Sort;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.EncodeVersion;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyType;
import com.netease.nim.camellia.redis.proxy.upstream.kv.utils.BytesUtils;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import com.netease.nim.camellia.tools.utils.BytesKey;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * ZREMRANGEBYLEX key min max
 * <p>
 * Created by caojiajun on 2024/5/8
 */
public class ZRemRangeByLexCommander extends ZRemRange0Commander {

    public ZRemRangeByLexCommander(CommanderConfig commanderConfig) {
        super(commanderConfig);
    }

    @Override
    public RedisCommand redisCommand() {
        return RedisCommand.ZREMRANGEBYLEX;
    }

    @Override
    protected boolean parse(Command command) {
        byte[][] objects = command.getObjects();
        return objects.length == 4;
    }

    @Override
    protected Reply execute(Command command) {
        byte[][] objects = command.getObjects();
        byte[] key = objects[1];
        KeyMeta keyMeta = keyMetaServer.getKeyMeta(key);
        if (keyMeta == null) {
            return IntegerReply.REPLY_0;
        }
        if (keyMeta.getKeyType() != KeyType.zset) {
            return ErrorReply.WRONG_TYPE;
        }

        EncodeVersion encodeVersion = keyMeta.getEncodeVersion();

        if (encodeVersion == EncodeVersion.version_1) {
            return ErrorReply.COMMAND_NOT_SUPPORT_IN_CURRENT_KV_ENCODE_VERSION;
        }

        ZSetLex minLex;
        ZSetLex maxLex;
        try {
            minLex = ZSetLex.fromLex(objects[2]);
            maxLex = ZSetLex.fromLex(objects[3]);
            if (minLex == null || maxLex == null) {
                return new ErrorReply("ERR min or max not valid string range item");
            }
        } catch (Exception e) {
            ErrorLogCollector.collect(ZRemRangeByLexCommander.class, "zremrangebylex command syntax error, illegal min/max lex");
            return ErrorReply.SYNTAX_ERROR;
        }
        if (minLex.isMax() || maxLex.isMin()) {
            return MultiBulkReply.EMPTY;
        }

        KvCacheMonitor.Type type = null;

        Map<BytesKey, Double> localCacheResult = null;
        Result result = null;
        byte[] cacheKey = keyDesign.cacheKey(keyMeta, key);

        WriteBufferValue<RedisZSet> bufferValue = zsetWriteBuffer.get(cacheKey);
        if (bufferValue != null) {
            RedisZSet zSet = bufferValue.getValue();
            localCacheResult = zSet.zremrangeByLex(minLex, maxLex);
            //
            type = KvCacheMonitor.Type.write_buffer;
            KvCacheMonitor.writeBuffer(cacheConfig.getNamespace(), redisCommand().strRaw());
            if (localCacheResult != null && localCacheResult.isEmpty()) {
                return IntegerReply.REPLY_0;
            }
            result = zsetWriteBuffer.put(cacheKey, zSet);
        }

        if (cacheConfig.isZSetLocalCacheEnable()) {
            ZSetLRUCache zSetLRUCache = cacheConfig.getZSetLRUCache();

            if (localCacheResult == null) {
                localCacheResult = zSetLRUCache.zremrangeByLex(key, cacheKey, minLex, maxLex);
                if (localCacheResult != null) {
                    type = KvCacheMonitor.Type.local_cache;
                    KvCacheMonitor.localCache(cacheConfig.getNamespace(), redisCommand().strRaw());
                }
                if (localCacheResult != null && localCacheResult.isEmpty()) {
                    return IntegerReply.REPLY_0;
                }
            } else {
                zSetLRUCache.zremrangeByLex(key, cacheKey, minLex, maxLex);
            }

            if (localCacheResult == null) {
                boolean hotKey = zSetLRUCache.isHotKey(key);
                if (hotKey) {
                    RedisZSet zSet = loadLRUCache(keyMeta, key);
                    if (zSet != null) {
                        //
                        type = KvCacheMonitor.Type.kv_store;
                        KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());
                        //
                        zSetLRUCache.putZSetForWrite(key, cacheKey, zSet);
                        //
                        localCacheResult = zSet.zremrangeByLex(minLex, maxLex);

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

        if (type == null) {
            KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());
        }

        if (encodeVersion == EncodeVersion.version_0) {
            return zremrangeByLex(keyMeta, key, cacheKey, minLex, maxLex, localCacheResult, result);
        }

        return ErrorReply.INTERNAL_ERROR;
    }

    private Reply zremrangeByLex(KeyMeta keyMeta, byte[] key, byte[] cacheKey, ZSetLex minLex, ZSetLex maxLex,
                                 Map<BytesKey, Double> localCacheResult, Result result) {
        EncodeVersion encodeVersion = keyMeta.getEncodeVersion();

        Map<BytesKey, Double> toRemoveMembers = localCacheResult;
        if (toRemoveMembers == null) {
            toRemoveMembers = zrangeByLex(keyMeta, key, minLex, maxLex);
        }
        if (toRemoveMembers.isEmpty()) {
            return IntegerReply.REPLY_0;
        }

        int toRemoveSize = toRemoveMembers.size();

        if (encodeVersion == EncodeVersion.version_0) {
            byte[][] deleteStoreKeys = new byte[toRemoveSize*2][];
            int i = 0;
            for (Map.Entry<BytesKey, Double> entry : toRemoveMembers.entrySet()) {
                deleteStoreKeys[i] = keyDesign.zsetMemberSubKey1(keyMeta, key, entry.getKey().getKey());
                deleteStoreKeys[i+1] = keyDesign.zsetMemberSubKey2(keyMeta, key, entry.getKey().getKey(), BytesUtils.toBytes(entry.getValue()));
                i+=2;
            }
            if (result.isKvWriteDelayEnable()) {
                submitAsyncWriteTask(cacheKey, result, () -> kvClient.batchDelete(deleteStoreKeys));
            } else {
                kvClient.batchDelete(deleteStoreKeys);
            }
        } else if (encodeVersion == EncodeVersion.version_2) {
            List<byte[]> deleteStoreKeys = new ArrayList<>(toRemoveSize*2);

            byte[][] zremCmd = new byte[toRemoveSize + 2][];
            zremCmd[0] = RedisCommand.ZREM.raw();
            zremCmd[1] = cacheKey;

            List<byte[]> deleteCmd = new ArrayList<>(toRemoveSize + 1);
            deleteCmd.add(RedisCommand.DEL.raw());

            int i = 2;
            for (Map.Entry<BytesKey, Double> entry : toRemoveMembers.entrySet()) {
                deleteStoreKeys.add(keyDesign.zsetMemberSubKey1(keyMeta, key, entry.getKey().getKey()));
                Index index = Index.fromRaw(entry.getKey().getKey());
                if (index.isIndex()) {
                    deleteStoreKeys.add(keyDesign.zsetIndexSubKey(keyMeta, key, index));
                    deleteCmd.add(keyDesign.zsetMemberIndexCacheKey(keyMeta, key, index));
                }
                zremCmd[i] = index.getRef();
                i++;
            }

            List<Command> commands = new ArrayList<>(2);
            commands.add(new Command(zremCmd));
            if (deleteCmd.size() > 1) {
                commands.add(new Command(deleteCmd.toArray(new byte[0][0])));
            }

            List<CompletableFuture<Reply>> futures = cacheRedisTemplate.sendCommand(commands);

            if (result.isKvWriteDelayEnable()) {
                submitAsyncWriteTask(cacheKey, result, () -> kvClient.batchDelete(deleteStoreKeys.toArray(new byte[0][0])));
            } else {
                kvClient.batchDelete(deleteStoreKeys.toArray(new byte[0][0]));
            }

            List<Reply> replyList = sync(futures);
            for (Reply reply : replyList) {
                if (reply instanceof  ErrorReply) {
                    return reply;
                }
            }
        }

        int size = BytesUtils.toInt(keyMeta.getExtra());
        size = size - toRemoveSize;
        updateKeyMeta(keyMeta, key, size);

        return IntegerReply.parse(toRemoveSize);
    }

    private Map<BytesKey, Double> zrangeByLex(KeyMeta keyMeta, byte[] key, ZSetLex minLex, ZSetLex maxLex) {
        byte[] startKey;
        if (minLex.isMin()) {
            startKey = keyDesign.zsetMemberSubKey1(keyMeta, key, new byte[0]);
        } else {
            startKey = keyDesign.zsetMemberSubKey1(keyMeta, key, minLex.getLex());
        }
        byte[] endKey;
        if (maxLex.isMax()) {
            endKey = BytesUtils.nextBytes(keyDesign.zsetMemberSubKey1(keyMeta, key, new byte[0]));
        } else {
            if (maxLex.isExcludeLex()) {
                endKey = keyDesign.zsetMemberSubKey1(keyMeta, key, maxLex.getLex());
            } else {
                endKey = BytesUtils.nextBytes(keyDesign.zsetMemberSubKey1(keyMeta, key, maxLex.getLex()));
            }
        }

        Map<BytesKey, Double> map = new HashMap<>();

        int scanBatch = kvConfig.scanBatch();
        while (true) {
            List<KeyValue> scan = kvClient.scanByStartEnd(startKey, endKey, scanBatch, Sort.ASC, !minLex.isExcludeLex());
            if (scan.isEmpty()) {
                break;
            }
            for (KeyValue keyValue : scan) {
                if (keyValue == null || keyValue.getValue() == null) {
                    continue;
                }
                startKey = keyValue.getKey();
                byte[] member = keyDesign.decodeZSetMemberBySubKey1(keyValue.getKey(), key);
                boolean pass = ZSetLexUtil.checkLex(member, minLex, maxLex);
                if (!pass) {
                    continue;
                }
                map.put(new BytesKey(member), Utils.bytesToDouble(keyValue.getValue()));
            }
            if (scan.size() < scanBatch) {
                break;
            }
        }
        return map;
    }

}
