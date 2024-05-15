package com.netease.nim.camellia.redis.proxy.upstream.kv.command.hash;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.reply.BulkReply;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.MultiBulkReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.CommanderConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.EncodeVersion;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyType;
import com.netease.nim.camellia.tools.utils.BytesKey;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * HGETALL key
 * <p>
 * Created by caojiajun on 2024/4/7
 */
public class HGetAllCommander extends Hash0Commander {

    private static final byte[] script = ("local arg = redis.call('exists', KEYS[1]);\n" +
            "if tonumber(arg) == 1 then\n" +
            "\tlocal ret = redis.call('hgetall', KEYS[1]);\n" +
            "\tredis.call('pexpire', KEYS[1], ARGV[1]);\n" +
            "\treturn {'1', ret};\n" +
            "end\n" +
            "return {'2'};").getBytes(StandardCharsets.UTF_8);

    public HGetAllCommander(CommanderConfig commanderConfig) {
        super(commanderConfig);
    }

    @Override
    public RedisCommand redisCommand() {
        return RedisCommand.HGETALL;
    }

    @Override
    protected boolean parse(Command command) {
        byte[][] objects = command.getObjects();
        return objects.length == 2;
    }

    @Override
    protected Reply execute(Command command) {
        byte[][] objects = command.getObjects();
        byte[] key = objects[1];

        //meta
        KeyMeta keyMeta = keyMetaServer.getKeyMeta(key);
        if (keyMeta == null) {
            return MultiBulkReply.EMPTY;
        }
        if (keyMeta.getKeyType() != KeyType.hash) {
            return ErrorReply.WRONG_TYPE;
        }

        EncodeVersion encodeVersion = keyMeta.getEncodeVersion();
        if (encodeVersion == EncodeVersion.version_0 || encodeVersion == EncodeVersion.version_1) {
            Map<BytesKey, byte[]> map = hgetallFromKv(keyMeta, key);
            if (map.isEmpty()) {
                return MultiBulkReply.EMPTY;
            }
            Reply[] replies = new Reply[map.size() * 2];
            int i = 0;
            for (Map.Entry<BytesKey, byte[]> entry : map.entrySet()) {
                replies[i] = new BulkReply(entry.getKey().getKey());
                replies[i + 1] = new BulkReply(entry.getValue());
                i += 2;
            }
            return new MultiBulkReply(replies);
        }

        byte[] cacheKey = keyDesign.cacheKey(keyMeta, key);

        Reply reply = checkCache(script, cacheKey, new byte[][]{hgetallCacheMillis()});
        if (reply != null) {
            return reply;
        }

        Map<BytesKey, byte[]> map = hgetallFromKv(keyMeta, key);

        if (map.isEmpty()) {
            return MultiBulkReply.EMPTY;
        }

        ErrorReply errorReply = buildCache(cacheKey, map);
        if (errorReply != null) {
            return errorReply;
        }

        Reply[] replies = new Reply[map.size() * 2];
        int i = 0;
        for (Map.Entry<BytesKey, byte[]> entry : map.entrySet()) {
            replies[i] = new BulkReply(entry.getKey().getKey());
            replies[i + 1] = new BulkReply(entry.getValue());
            i += 2;
        }
        return new MultiBulkReply(replies);
    }

}
