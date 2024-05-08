package com.netease.nim.camellia.redis.proxy.upstream.kv.command.zset;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.IntegerReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.CommanderConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.EncodeVersion;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyType;

import java.nio.charset.StandardCharsets;

/**
 * ZREMRANGEBYLEX key min max
 * <p>
 * Created by caojiajun on 2024/5/8
 */
public class ZRemRangeByLexCommander extends ZRemRange0Commander {

    private static final byte[] script = ("local ret1 = redis.call('exists', KEYS[1]);\n" +
            "if ret1 then\n" +
            "  local ret = redis.call('zrangebylex', KEYS[1], unpack(ARGV));\n" +
            "  return {'2', ret};\n" +
            "end\n" +
            "return {'1'};").getBytes(StandardCharsets.UTF_8);

    public ZRemRangeByLexCommander(CommanderConfig commanderConfig) {
        super(commanderConfig);
    }

    @Override
    public RedisCommand redisCommand() {
        return RedisCommand.ZREMRANGEBYRANK;
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
            return zremrangeByLex(keyMeta, key, objects);
        }
        byte[][] zrangeArgs = new byte[objects.length][];
        System.arraycopy(objects, 0, zrangeArgs, 0, zrangeArgs.length);
        zrangeArgs[0] = RedisCommand.ZRANGEBYLEX.raw();
        if (encodeVersion == EncodeVersion.version_1) {
            return zremrangeVersion1(keyMeta, key, zrangeArgs, script);
        }
        if (encodeVersion == EncodeVersion.version_2) {
            return zremrangeVersion2(keyMeta, key, zrangeArgs, script);
        }
        if (encodeVersion == EncodeVersion.version_3) {
            return zremrangeVersion3(keyMeta, key, zrangeArgs);
        }
        return ErrorReply.INTERNAL_ERROR;
    }

    private Reply zremrangeByLex(KeyMeta keyMeta, byte[] key, byte[][] objects) {
        return ErrorReply.SYNTAX_ERROR;
    }
}
