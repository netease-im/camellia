package com.netease.nim.camellia.redis.proxy.upstream.kv.command.zset;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.MultiBulkReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.CommanderConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.EncodeVersion;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyType;
import com.netease.nim.camellia.redis.proxy.util.Utils;

import java.nio.charset.StandardCharsets;

/**
 * ZREVRANGEBYSCORE key max min [WITHSCORES] [LIMIT offset count]
 * <p>
 * Created by caojiajun on 2024/4/11
 */
public class ZRevRangeByScoreCommander extends ZRange0Commander {

    private static final byte[] script = ("local ret1 = redis.call('exists', KEYS[1]);\n" +
            "if ret1 then\n" +
            "  local ret = redis.call('zrevrangebyscore', KEYS[1], unpack(ARGV));\n" +
            "  return {'2', ret};\n" +
            "end\n" +
            "return {'1'};").getBytes(StandardCharsets.UTF_8);

    public ZRevRangeByScoreCommander(CommanderConfig commanderConfig) {
        super(commanderConfig);
    }

    @Override
    public RedisCommand redisCommand() {
        return RedisCommand.ZREVRANGEBYSCORE;
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
        if (objects.length >= 5) {
            for (int i=4; i<objects.length; i++) {
                withScores = Utils.bytesToString(objects[i]).equalsIgnoreCase("withscores");
                if (withScores) {
                    break;
                }
            }
        }
        EncodeVersion encodeVersion = keyMeta.getEncodeVersion();
        if (encodeVersion == EncodeVersion.version_0) {
            return zrevrangeByScoreVersion0(keyMeta, key, objects, withScores);
        }
        if (encodeVersion == EncodeVersion.version_1) {
            return zrangeVersion1(keyMeta, key, objects, script);
        }
        if (encodeVersion == EncodeVersion.version_2) {
            return zrangeVersion2(keyMeta, key, objects, withScores, script, true);
        }
        if (encodeVersion == EncodeVersion.version_3) {
            return zrangeVersion3(keyMeta, key, objects, withScores);
        }
        return ErrorReply.INTERNAL_ERROR;
    }

    private Reply zrevrangeByScoreVersion0(KeyMeta keyMeta, byte[] key, byte[][] objects, boolean withScores) {
        return ErrorReply.SYNTAX_ERROR;
    }



}