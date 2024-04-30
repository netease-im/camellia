package com.netease.nim.camellia.redis.proxy.upstream.kv.command.zset;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.reply.BulkReply;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.MultiBulkReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.Commander;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.CommanderConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.domain.Index;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.KeyValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.EncodeVersion;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyType;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import com.netease.nim.camellia.tools.utils.BytesKey;

import java.util.*;

/**
 * ZRANGE key start stop [WITHSCORES]
 * <p>
 * Created by caojiajun on 2024/4/11
 */
public class ZRangeCommander extends Commander {

    public ZRangeCommander(CommanderConfig commanderConfig) {
        super(commanderConfig);
    }

    @Override
    public RedisCommand redisCommand() {
        return RedisCommand.ZRANGE;
    }

    @Override
    protected boolean parse(Command command) {
        byte[][] objects = command.getObjects();
        return objects.length >= 4;
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
        boolean withScores = false;
        if (objects.length == 5) {
            withScores = Utils.bytesToString(objects[4]).equalsIgnoreCase("withscores");
            if (!withScores) {
                return ErrorReply.SYNTAX_ERROR;
            }
        }
        EncodeVersion encodeVersion = keyMeta.getEncodeVersion();
        if (encodeVersion == EncodeVersion.version_0) {
            return null;
        }
        if (encodeVersion == EncodeVersion.version_1) {
            return null;
        }
        if (encodeVersion == EncodeVersion.version_2) {
            return null;
        }
        if (encodeVersion == EncodeVersion.version_3 || encodeVersion == EncodeVersion.version_4) {
            return zrangeVersion3Or4(keyMeta, key, objects, withScores);
        }
        return ErrorReply.INTERNAL_ERROR;
    }

    private Reply zrangeVersion3Or4(KeyMeta keyMeta, byte[] key, byte[][] objects, boolean withScores) {
        byte[] cacheKey = keyStruct.cacheKey(keyMeta, key);
        byte[][] cmd = new byte[objects.length][];
        cmd[0] = objects[0];
        cmd[1] = cacheKey;
        System.arraycopy(objects, 2, cmd, 2, objects.length - 2);
        Reply reply = sync(storeRedisTemplate.sendCommand(new Command(cmd)));
        if (reply instanceof ErrorReply) {
            return reply;
        }
        if (reply instanceof MultiBulkReply) {
            Reply[] replies = ((MultiBulkReply) reply).getReplies();
            int step = withScores ? 2: 1;
            int size = withScores ? replies.length/2 : replies.length;
            Map<BytesKey, byte[]> memberMap = new HashMap<>(size);
            Map<BytesKey, Index> missingMemberMap = new HashMap<>();
            for (int i=0; i<replies.length; i+=step) {
                Reply reply1 = replies[i];
                if (reply1 instanceof BulkReply) {
                    byte[] data = ((BulkReply) reply1).getRaw();
                    Index index = Index.fromData(data);
                    if (!index.isIndex()) {
                        memberMap.put(new BytesKey(data), index.getRaw());
                    } else {
                        missingMemberMap.put(new BytesKey(data), index);
                    }
                }
            }
            if (!missingMemberMap.isEmpty()) {
                BytesKey[] bytesKeys = missingMemberMap.keySet().toArray(new BytesKey[0]);
                List<Command> commandList = new ArrayList<>(bytesKeys.length);
                for (BytesKey bytesKey : bytesKeys) {
                    Index index = missingMemberMap.get(bytesKey);
                    byte[] memberCacheKey = keyStruct.zsetMemberIndexCacheKey(keyMeta, key, index);
                    Command cmd1 = new Command(new byte[][]{RedisCommand.GET.raw(), memberCacheKey});
                    commandList.add(cmd1);
                }
                List<Reply> replyList = sync(cacheRedisTemplate.sendCommand(commandList));
                for (int i = 0; i < replyList.size(); i++) {
                    Reply subReply = replyList.get(i);
                    BytesKey member = bytesKeys[i];
                    if (subReply instanceof ErrorReply) {
                        return reply;
                    }
                    if (subReply instanceof BulkReply) {
                        byte[] raw = ((BulkReply) subReply).getRaw();
                        if (raw != null && raw.length > 0) {
                            memberMap.put(member, raw);
                            missingMemberMap.remove(member);
                        }
                    }
                }
            }
            if (!missingMemberMap.isEmpty()) {
                byte[][] batchGetKeys = new byte[missingMemberMap.size()][];
                int i=0;
                Map<BytesKey, BytesKey> reverseMap = new HashMap<>(missingMemberMap.size());
                for (Map.Entry<BytesKey, Index> entry : missingMemberMap.entrySet()) {
                    Index index = entry.getValue();
                    byte[] subKey = keyStruct.zsetIndexSubKey(keyMeta, key, index);
                    batchGetKeys[i] = subKey;
                    i++;
                    reverseMap.put(new BytesKey(subKey), entry.getKey());
                }
                List<Command> buildCacheCommands = new ArrayList<>(batchGetKeys.length);
                List<KeyValue> keyValues = kvClient.batchGet(batchGetKeys);
                for (KeyValue keyValue : keyValues) {
                    BytesKey bytesKey = reverseMap.get(new BytesKey(keyValue.getKey()));
                    memberMap.put(bytesKey, keyValue.getValue());

                    Index index = missingMemberMap.get(bytesKey);
                    byte[] memberCacheKey = keyStruct.zsetMemberIndexCacheKey(keyMeta, key, index);
                    buildCacheCommands.add(new Command(new byte[][]{RedisCommand.PSETEX.raw(), memberCacheKey, zsetMemberCacheMillis(), keyValue.getValue()}));

                    missingMemberMap.remove(bytesKey);
                }
                if (!buildCacheCommands.isEmpty()) {
                    List<Reply> replyList = sync(cacheRedisTemplate.sendCommand(buildCacheCommands));
                    for (Reply reply1 : replyList) {
                        if (reply1 instanceof ErrorReply) {
                            return reply1;
                        }
                    }
                }
            }
            if (!missingMemberMap.isEmpty()) {
                return ErrorReply.INTERNAL_ERROR;
            }
            Reply[] result = new Reply[replies.length];
            for (int i=0; i<replies.length; i++) {
                if (withScores) {
                    if (i % 2 == 1) {
                        result[i] = replies[i];
                        continue;
                    }
                }
                Reply reply1 = replies[i];
                if (reply1 instanceof BulkReply) {
                    byte[] data = ((BulkReply) reply1).getRaw();
                    byte[] bytes = memberMap.get(new BytesKey(data));
                    result[i] = new BulkReply(bytes);
                } else {
                    return ErrorReply.INTERNAL_ERROR;
                }
            }
            return new MultiBulkReply(result);
        }
        return ErrorReply.SYNTAX_ERROR;
    }

    private byte[] zsetMemberCacheMillis() {
        return Utils.stringToBytes(String.valueOf(cacheConfig.zsetMemberCacheMillis()));
    }
}
