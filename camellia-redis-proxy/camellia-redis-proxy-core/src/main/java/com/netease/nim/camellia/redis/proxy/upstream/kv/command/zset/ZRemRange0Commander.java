package com.netease.nim.camellia.redis.proxy.upstream.kv.command.zset;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.reply.*;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.CommanderConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.domain.Index;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Created by caojiajun on 2024/5/8
 */
public abstract class ZRemRange0Commander extends ZRange0Commander {

    public ZRemRange0Commander(CommanderConfig commanderConfig) {
        super(commanderConfig);
    }

    protected final Reply zremrangeVersion1(KeyMeta keyMeta, byte[] key, byte[][] zrangeArgs, byte[] script) {
        Reply reply = zrangeVersion1(keyMeta, key, zrangeArgs, script);
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
            zremCmd[1] = keyStruct.cacheKey(keyMeta, key);
            int i = 2;
            byte[][] delKeys = new byte[replies.length][];
            for (Reply reply1 : replies) {
                if (reply1 instanceof BulkReply) {
                    byte[] member = ((BulkReply) reply1).getRaw();
                    zremCmd[i] = member;
                    delKeys[i-2] = keyStruct.zsetMemberSubKey1(keyMeta, key, member);
                } else {
                    return ErrorReply.INTERNAL_ERROR;
                }
                i++;
            }
            CompletableFuture<Reply> future = cacheRedisTemplate.sendCommand(new Command(zremCmd));
            kvClient.batchDelete(delKeys);
            Reply reply1 = sync(future);
            if (reply1 instanceof ErrorReply) {
                return reply1;
            }
            return IntegerReply.parse(replies.length);
        }
        return ErrorReply.INTERNAL_ERROR;
    }

    protected final Reply zremrangeVersion2(KeyMeta keyMeta, byte[] key, byte[][] zrangeArgs, byte[] script) {
        Reply reply = zrangeVersion2(keyMeta, key, zrangeArgs, false, script, false);
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
            zremCmd[1] = keyStruct.cacheKey(keyMeta, key);
            int i = 2;
            List<byte[]> delCacheKeys = new ArrayList<>(replies.length + 1);
            delCacheKeys.add(RedisCommand.DEL.raw());
            List<byte[]> delStoreKeys = new ArrayList<>(replies.length * 2);
            for (Reply reply1 : replies) {
                if (reply1 instanceof BulkReply) {
                    byte[] member = ((BulkReply) reply1).getRaw();
                    Index index = Index.fromRaw(member);
                    zremCmd[i] = index.getRef();
                    delStoreKeys.add(keyStruct.zsetMemberSubKey1(keyMeta, key, member));
                    if (index.isIndex()) {
                        delStoreKeys.add(keyStruct.zsetIndexSubKey(keyMeta, key, index));
                        delCacheKeys.add(keyStruct.zsetMemberIndexCacheKey(keyMeta, key, index));
                    }
                } else {
                    return ErrorReply.INTERNAL_ERROR;
                }
                i++;
            }
            List<Command> cmds = new ArrayList<>(2);
            cmds.add(new Command(zremCmd));
            if (delCacheKeys.size() > 1) {
                cmds.add(new Command(delCacheKeys.toArray(new byte[0][0])));
            }
            List<CompletableFuture<Reply>> futures = cacheRedisTemplate.sendCommand(cmds);
            kvClient.batchDelete(delStoreKeys.toArray(new byte[0][0]));
            List<Reply> replyList = sync(futures);
            for (Reply reply1 : replyList) {
                if (reply1 instanceof ErrorReply) {
                    return reply1;
                }
            }
            return IntegerReply.parse(replies.length);
        }
        return ErrorReply.INTERNAL_ERROR;
    }

    protected final Reply zremrangeVersion3(KeyMeta keyMeta, byte[] key, byte[][] zrangeArgs) {
        Reply reply = sync(storeRedisTemplate.sendCommand(new Command(zrangeArgs)));
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
            zremCmd[1] = keyStruct.cacheKey(keyMeta, key);
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
                        delStoreKeys.add(keyStruct.zsetIndexSubKey(keyMeta, key, index));
                        delCacheKeys.add(keyStruct.zsetMemberIndexCacheKey(keyMeta, key, index));
                    }
                } else {
                    return ErrorReply.INTERNAL_ERROR;
                }
                i++;
            }
            List<Command> cmds = new ArrayList<>(2);
            cmds.add(new Command(zremCmd));
            if (delCacheKeys.size() > 1) {
                cmds.add(new Command(delCacheKeys.toArray(new byte[0][0])));
            }
            List<CompletableFuture<Reply>> futures = cacheRedisTemplate.sendCommand(cmds);
            if (!delStoreKeys.isEmpty()) {
                kvClient.batchDelete(delStoreKeys.toArray(new byte[0][0]));
            }
            List<Reply> replyList = sync(futures);
            for (Reply reply1 : replyList) {
                if (reply1 instanceof ErrorReply) {
                    return reply1;
                }
            }
            return IntegerReply.parse(replies.length);
        }
        return ErrorReply.INTERNAL_ERROR;
    }
}
