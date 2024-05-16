package com.netease.nim.camellia.redis.proxy.upstream.kv.command.zset;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.IntegerReply;
import com.netease.nim.camellia.redis.proxy.reply.MultiBulkReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.CommanderConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.domain.Index;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.KeyValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.Sort;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.EncodeVersion;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyType;
import com.netease.nim.camellia.redis.proxy.upstream.kv.utils.BytesUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * ZREMRANGEBYLEX key min max
 * <p>
 * Created by caojiajun on 2024/5/8
 */
public class ZRemRangeByLexCommander extends ZRemRange0Commander {

    private static final byte[] script = ("local ret1 = redis.call('exists', KEYS[1]);\n" +
            "if tonumber(ret1) == 1 then\n" +
            "  local ret = redis.call('zrangebylex', KEYS[1], unpack(ARGV));\n" +
            "  return {'1', ret};\n" +
            "end\n" +
            "return {'2'};").getBytes(StandardCharsets.UTF_8);

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
        if (encodeVersion == EncodeVersion.version_0) {
            return zremrangeByLexVersion0OrVersion1(keyMeta, key, objects);
        }
        if (encodeVersion == EncodeVersion.version_2) {
            return zremrangeByLexVersion0OrVersion1(keyMeta, key, objects);
        }
        byte[][] zrangeArgs = new byte[objects.length][];
        System.arraycopy(objects, 0, zrangeArgs, 0, zrangeArgs.length);
        zrangeArgs[0] = RedisCommand.ZRANGEBYLEX.raw();
        if (encodeVersion == EncodeVersion.version_1) {
            return zremrangeVersion1(keyMeta, key, zrangeArgs, script);
        }
        if (encodeVersion == EncodeVersion.version_3) {
            return ErrorReply.COMMAND_NOT_SUPPORT_IN_CURRENT_KV_ENCODE_VERSION;
        }
        return ErrorReply.INTERNAL_ERROR;
    }

    private Reply zremrangeByLexVersion0OrVersion1(KeyMeta keyMeta, byte[] key, byte[][] objects) {
        EncodeVersion encodeVersion = keyMeta.getEncodeVersion();
        ZSetLex minLex;
        ZSetLex maxLex;
        try {
            minLex = ZSetLex.fromLex(objects[2]);
            maxLex = ZSetLex.fromLex(objects[3]);
            if (minLex == null || maxLex == null) {
                return new ErrorReply("ERR min or max not valid string range item");
            }
        } catch (Exception e) {
            return ErrorReply.SYNTAX_ERROR;
        }
        if (minLex.isMax() || maxLex.isMin()) {
            return MultiBulkReply.EMPTY;
        }
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
        List<byte[]> toDeleteKeys = new ArrayList<>();
        List<byte[]> toDeleteRedisKeys = new ArrayList<>();
        int scanBatch = kvConfig.scanBatch();
        int count = 0;
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
                toDeleteKeys.add(keyValue.getKey());
                if (encodeVersion == EncodeVersion.version_0) {
                    toDeleteKeys.add(keyDesign.zsetMemberSubKey2(keyMeta, key, member, keyValue.getValue()));
                }
                if (encodeVersion == EncodeVersion.version_2) {
                    Index index = Index.fromRaw(member);
                    if (index.isIndex()) {
                        toDeleteKeys.add(keyDesign.zsetIndexSubKey(keyMeta, key, index));
                        byte[] indexCacheKey = keyDesign.zsetMemberIndexCacheKey(keyMeta, key, index);
                        toDeleteRedisKeys.add(indexCacheKey);
                    }
                }
                count++;
            }
            if (scan.size() < scanBatch) {
                break;
            }
        }
        if (!toDeleteKeys.isEmpty()) {
            int size = BytesUtils.toInt(keyMeta.getExtra());
            size = size - count;
            if (size <= 0) {
                toDeleteRedisKeys.add(keyDesign.cacheKey(keyMeta, key));
            }

            CompletableFuture<Reply> future = cacheRedisTemplate.sendDel(toDeleteRedisKeys.toArray(new byte[0][0]));

            kvClient.batchDelete(toDeleteKeys.toArray(new byte[0][0]));

            updateKeyMeta(keyMeta, key, size);

            if (future != null) {
                Reply reply = sync(future);
                if (reply instanceof ErrorReply) {
                    return reply;
                }
            }
            return IntegerReply.parse(count);
        }
        return IntegerReply.REPLY_0;
    }

}
