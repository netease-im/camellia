package com.netease.nim.camellia.redis.proxy.upstream.kv.command.zset;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.reply.*;
import com.netease.nim.camellia.redis.proxy.upstream.kv.buffer.Result;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.CommanderConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.domain.Index;
import com.netease.nim.camellia.redis.proxy.upstream.kv.exception.KvException;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.upstream.kv.utils.BytesUtils;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import com.netease.nim.camellia.tools.utils.BytesKey;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Created by caojiajun on 2024/5/8
 */
public abstract class ZRem0Commander extends ZRange0Commander {

    public ZRem0Commander(CommanderConfig commanderConfig) {
        super(commanderConfig);
    }

    protected final Reply zremVersion0(KeyMeta keyMeta, byte[] key, byte[] cacheKey, Map<BytesKey, Double> removedMembers, Result result) {
        List<byte[]> delStoreKeys = new ArrayList<>(removedMembers.size() * 2);
        for (Map.Entry<BytesKey, Double> entry : removedMembers.entrySet()) {
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

        int size = BytesUtils.toInt(keyMeta.getExtra());
        size = size - removedMembers.size();
        updateKeyMeta(keyMeta, key, size);

        return IntegerReply.parse(removedMembers.size());
    }

    protected final Reply zremrangeVersion1(KeyMeta keyMeta, byte[] key, byte[] cacheKey, byte[][] objects, RedisCommand redisCommand,
                                            Map<BytesKey, Double> removedMap, Result result) {
        Set<BytesKey> removedMembers;
        if (removedMap == null) {
            removedMembers = zremrangeMembers(cacheKey, objects, redisCommand);
        } else {
            removedMembers = removedMap.keySet();
        }
        if (removedMembers == null) {
            ErrorLogCollector.collect(ZRem0Commander.class, "zremrangeMembers error");
            return ErrorReply.INTERNAL_ERROR;
        }
        return zremVersion1(keyMeta, key, cacheKey, removedMembers, result);
    }

    private Set<BytesKey> zremrangeMembers(byte[] cacheKey, byte[][] objects, RedisCommand redisCommand) {
        byte[][] cmd = new byte[objects.length][];
        System.arraycopy(objects, 0, cmd, 0, objects.length);
        if (redisCommand == RedisCommand.ZREMRANGEBYSCORE) {
            cmd[0] = RedisCommand.ZRANGEBYSCORE.raw();
        } else if (redisCommand == RedisCommand.ZREMRANGEBYRANK) {
            cmd[0] = RedisCommand.ZRANGE.raw();
        } else {
            throw new KvException("only support ZREMRANGEBYSCORE/ZREMRANGEBYRANK");
        }
        cmd[1] = cacheKey;
        Reply reply = sync(storageRedisTemplate.sendCommand(new Command(cmd)));
        if (reply instanceof ErrorReply) {
            return null;
        }
        if (reply instanceof MultiBulkReply) {
            Reply[] replies = ((MultiBulkReply) reply).getReplies();
            if (replies.length == 0) {
                return Collections.emptySet();
            }
            Set<BytesKey> removedMembers = new HashSet<>(replies.length);
            for (Reply reply1 : replies) {
                if (reply1 instanceof BulkReply) {
                    byte[] member = ((BulkReply) reply1).getRaw();
                    removedMembers.add(new BytesKey(member));
                } else {
                    return null;
                }
            }
            return removedMembers;
        }
        return null;
    }

    protected final Reply zremVersion1(KeyMeta keyMeta, byte[] key, byte[] cacheKey, Set<BytesKey> removedMembers, Result result) {
        int size = removedMembers.size();
        List<byte[]> deleteSubKeys = new ArrayList<>(size);

        List<CompletableFuture<Reply>> futures = null;
        CompletableFuture<Reply> future = null;

        if (size > 0) {
            byte[][] zremCmd = new byte[size + 2][];
            zremCmd[0] = RedisCommand.ZREM.raw();
            zremCmd[1] = cacheKey;
            int i = 2;
            List<byte[]> deleteCacheKeys = new ArrayList<>(size + 1);
            deleteCacheKeys.add(RedisCommand.DEL.raw());
            for (BytesKey member : removedMembers) {
                Index index = Index.fromRaw(member.getKey());
                zremCmd[i] = index.getRef();
                if (index.isIndex()) {
                    deleteSubKeys.add(keyDesign.zsetIndexSubKey(keyMeta, key, index));
                    deleteCacheKeys.add(keyDesign.zsetMemberIndexCacheKey(keyMeta, key, index));
                }
                i++;
            }
            List<Command> cmds = new ArrayList<>(2);
            cmds.add(new Command(zremCmd));
            cmds.add(new Command(new byte[][]{RedisCommand.ZCARD.raw(), cacheKey}));

            //store redis
            futures = storageRedisTemplate.sendCommand(cmds);
            //cache redis
            if (deleteCacheKeys.size() > 1) {
                future = cacheRedisTemplate.sendCommand(new Command(deleteCacheKeys.toArray(new byte[0][0])));
            }
        }

        if (result.isKvWriteDelayEnable()) {
            submitAsyncWriteTask(cacheKey, result, () -> {
                if (!deleteSubKeys.isEmpty()) {
                    kvClient.batchDelete(deleteSubKeys.toArray(new byte[0][0]));
                }
            });
        } else {
            if (!deleteSubKeys.isEmpty()) {
                kvClient.batchDelete(deleteSubKeys.toArray(new byte[0][0]));
            }
        }

        if (future != null) {
            sync(future);
        }

        if (futures != null) {
            List<Reply> replyList = sync(futures);
            for (Reply reply1 : replyList) {
                if (reply1 instanceof ErrorReply) {
                    return reply1;
                }
            }
            Reply reply1 = replyList.get(1);
            if (reply1 instanceof IntegerReply) {
                if (((IntegerReply) reply1).getInteger() == 0) {
                    keyMetaServer.deleteKeyMeta(key);
                }
            }
            return replyList.get(0);
        }
        return IntegerReply.parse(size);
    }

    protected final void updateKeyMeta(KeyMeta keyMeta, byte[] key, int size) {
        if (size <= 0) {
            keyMetaServer.deleteKeyMeta(key);
            return;
        }
        byte[] extra = BytesUtils.toBytes(size);
        keyMeta = new KeyMeta(keyMeta.getEncodeVersion(), keyMeta.getKeyType(), keyMeta.getKeyVersion(), keyMeta.getExpireTime(), extra);
        keyMetaServer.createOrUpdateKeyMeta(key, keyMeta);
    }
}
