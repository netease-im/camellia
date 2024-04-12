package com.netease.nim.camellia.redis.proxy.kv.core.command.hash;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.kv.core.command.Commander;
import com.netease.nim.camellia.redis.proxy.kv.core.command.CommanderConfig;
import com.netease.nim.camellia.redis.proxy.kv.core.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.kv.core.meta.KeyType;
import com.netease.nim.camellia.redis.proxy.kv.core.kv.KeyValue;
import com.netease.nim.camellia.redis.proxy.reply.BulkReply;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.MultiBulkReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.util.Utils;

import java.nio.charset.StandardCharsets;

/**
 * HGET key field
 * <p>
 * Created by caojiajun on 2024/4/7
 */
public class HGetCommander extends Commander {

    public HGetCommander(CommanderConfig commanderConfig) {
        super(commanderConfig);
    }

    //get hget cache, if exists, delay ttl, return 1 and value
    //get hgetall cache, if exists, delay ttl, return 2 and value
    //return 3 and value
    private static final byte[] script = ("local ret1 = redis.call('get', KEYS[1]);\n" +
            "if ret1 then\n" +
            "\tredis.call('pexpire', KEYS[1], ARGV[2]);\n" +
            "\treturn {'1', ret1};\n" +
            "end\n" +
            "local arg1 = redis.call('exists', KEYS[2]);\n" +
            "if tonumber(arg1) == 1 then\n" +
            "\tlocal ret2 = redis.call('hget', KEYS[2], ARGV[1]);\n" +
            "\tredis.call('pexpire', KEYS[2], ARGV[3]);\n" +
            "\treturn {'2', ret2};\n" +
            "end\n" +
            "return {'3'};").getBytes(StandardCharsets.UTF_8);

    @Override
    public RedisCommand redisCommand() {
        return RedisCommand.HGET;
    }

    @Override
    protected boolean parse(Command command) {
        byte[][] objects = command.getObjects();
        return objects.length == 3;
    }

    @Override
    protected Reply execute(Command command) {
        byte[][] objects = command.getObjects();
        byte[] key = objects[1];
        byte[] field = objects[2];

        //meta
        KeyMeta keyMeta = keyMetaServer.getKeyMeta(key);
        if (keyMeta == null) {
            return BulkReply.NIL_REPLY;
        }
        if (keyMeta.getKeyType() != KeyType.hash) {
            return ErrorReply.WRONG_TYPE;
        }

        byte[] hashFieldCacheKey = keyStruct.hashFieldCacheKey(keyMeta, key, field);
        byte[] cacheKey = keyStruct.cacheKey(keyMeta, key);

        //cache
        {
            Reply reply = sync(cacheRedisTemplate.sendLua(script, new byte[][]{hashFieldCacheKey, cacheKey},
                    new byte[][]{field, hgetCacheMillis(), hgetallCacheMillis()}));
            if (reply instanceof ErrorReply) {
                return reply;
            }
            if (reply instanceof MultiBulkReply) {
                Reply[] replies = ((MultiBulkReply) reply).getReplies();
                String type = Utils.bytesToString(((BulkReply) replies[0]).getRaw());
                if (type.equalsIgnoreCase("1") || type.equalsIgnoreCase("2")) {
                    return replies[1];
                }
            }
        }

        //store
        byte[] storeKey = keyStruct.hashFieldStoreKey(keyMeta, key, field);
        KeyValue keyValue = kvClient.get(storeKey);

        if (keyValue == null || keyValue.getValue() == null) {
            return BulkReply.NIL_REPLY;
        }

        //build hget cache
        Reply reply = sync(cacheRedisTemplate.sendPSetEx(hashFieldCacheKey, cacheConfig.hgetCacheMillis(), keyValue.getValue()));
        if (reply instanceof ErrorReply) {
            return reply;
        }
        return new BulkReply(keyValue.getValue());
    }

    private byte[] hgetallCacheMillis() {
        return Utils.stringToBytes(String.valueOf(cacheConfig.hgetallCacheMillis()));
    }

    private byte[] hgetCacheMillis() {
        return Utils.stringToBytes(String.valueOf(cacheConfig.hgetCacheMillis()));
    }
}
