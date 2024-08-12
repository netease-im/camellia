package com.netease.nim.camellia.redis.proxy.upstream.kv.command.zset;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.reply.*;
import com.netease.nim.camellia.redis.proxy.upstream.kv.buffer.Result;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.CommanderConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.domain.Index;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.upstream.kv.utils.BytesUtils;
import com.netease.nim.camellia.tools.utils.BytesKey;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Created by caojiajun on 2024/5/8
 */
public abstract class ZRemRange0Commander extends ZRange0Commander {

    public ZRemRange0Commander(CommanderConfig commanderConfig) {
        super(commanderConfig);
    }

    protected final Reply zremrangeVersion1(KeyMeta keyMeta, byte[] key, byte[] cacheKey, byte[][] objects, RedisCommand redisCommand,
                                            Map<BytesKey, Double> localCacheResult, Result result) {
        if (localCacheResult != null) {
            byte[][] zremCmd = new byte[localCacheResult.size() + 2][];
            zremCmd[0] = RedisCommand.ZREM.raw();
            zremCmd[1] = cacheKey;
            int i = 2;
            List<byte[]> delCacheKeys = new ArrayList<>(localCacheResult.size() * 2);
            List<byte[]> delStoreKeys = new ArrayList<>(localCacheResult.size() * 2);
            for (Map.Entry<BytesKey, Double> entry : localCacheResult.entrySet()) {
                byte[] member = entry.getKey().getKey();
                Index index = Index.fromRaw(member);
                delStoreKeys.add(keyDesign.zsetMemberSubKey1(keyMeta, key, member));
                if (index.isIndex()) {
                    delStoreKeys.add(keyDesign.zsetIndexSubKey(keyMeta, key, index));
                    delCacheKeys.add(keyDesign.zsetMemberIndexCacheKey(keyMeta, key, index));
                }
                zremCmd[i] = index.getRef();
                i ++;
            }
            CompletableFuture<Reply> future1 = redisTemplate.sendCommand(new Command(zremCmd));
            CompletableFuture<Reply> future2 = null;
            if (!delCacheKeys.isEmpty()) {
                List<byte[]> delCmd = new ArrayList<>(delCacheKeys.size() + 1);
                delCmd.add(RedisCommand.DEL.raw());
                delCmd.addAll(delCacheKeys);
                future2 = redisTemplate.sendCommand(new Command(delCmd.toArray(new byte[0][0])));
            }

            if (result.isKvWriteDelayEnable()) {
                submitAsyncWriteTask(cacheKey, result, () -> kvClient.batchDelete(delStoreKeys.toArray(new byte[0][0])));
            } else {
                kvClient.batchDelete(delStoreKeys.toArray(new byte[0][0]));
            }

            Reply reply1 = sync(future1);
            if (reply1 instanceof ErrorReply) {
                return reply1;
            }
            if (future2 != null) {
                Reply reply2 = sync(future2);
                if (reply2 instanceof ErrorReply) {
                    return reply2;
                }
            }
            return IntegerReply.parse(localCacheResult.size());
        }

        byte[][] cmd = new byte[objects.length][];
        System.arraycopy(objects, 0, cmd, 0, objects.length);
        if (redisCommand == RedisCommand.ZREMRANGEBYSCORE) {
            cmd[0] = RedisCommand.ZRANGEBYSCORE.raw();
        } else if (redisCommand == RedisCommand.ZREMRANGEBYRANK) {
            cmd[0] = RedisCommand.ZRANGE.raw();
        } else {
            return ErrorReply.INTERNAL_ERROR;
        }
        cmd[1] = cacheKey;
        Reply reply = sync(redisTemplate.sendCommand(new Command(cmd)));
        if (reply instanceof ErrorReply) {
            return reply;
        }
        if (reply instanceof MultiBulkReply) {
            Reply[] replies = ((MultiBulkReply) reply).getReplies();
            if (replies.length == 0) {
                return IntegerReply.REPLY_0;
            }
            byte[][] zremCmd = new byte[replies.length + 2][];
            zremCmd[0] = RedisCommand.ZREM.raw();
            zremCmd[1] = cacheKey;
            int i = 2;
            List<byte[]> delCacheKeys = new ArrayList<>(replies.length * 2 + 1);
            delCacheKeys.add(RedisCommand.DEL.raw());
            List<byte[]> delStoreKeys = new ArrayList<>(replies.length * 2);
            for (Reply reply1 : replies) {
                if (reply1 instanceof BulkReply) {
                    byte[] member = ((BulkReply) reply1).getRaw();
                    Index index = Index.fromRef(member);
                    zremCmd[i] = member;
                    if (index.isIndex()) {
                        delStoreKeys.add(keyDesign.zsetIndexSubKey(keyMeta, key, index));
                        delCacheKeys.add(keyDesign.zsetMemberIndexCacheKey(keyMeta, key, index));
                    }
                } else {
                    return ErrorReply.INTERNAL_ERROR;
                }
                i++;
            }
            List<Command> cmds = new ArrayList<>(2);
            cmds.add(new Command(zremCmd));
            cmds.add(new Command(new byte[][]{RedisCommand.ZCARD.raw(), cacheKey}));
            if (delCacheKeys.size() > 1) {
                cmds.add(new Command(delCacheKeys.toArray(new byte[0][0])));
            }
            List<CompletableFuture<Reply>> futures = redisTemplate.sendCommand(cmds);
            if (!delStoreKeys.isEmpty()) {
                kvClient.batchDelete(delStoreKeys.toArray(new byte[0][0]));
            }
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
            return IntegerReply.parse(replies.length);
        }
        return ErrorReply.INTERNAL_ERROR;
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
