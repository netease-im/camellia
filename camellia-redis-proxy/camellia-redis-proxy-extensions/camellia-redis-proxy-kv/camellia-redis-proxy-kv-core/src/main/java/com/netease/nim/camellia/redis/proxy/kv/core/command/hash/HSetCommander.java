package com.netease.nim.camellia.redis.proxy.kv.core.command.hash;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.kv.core.command.Commander;
import com.netease.nim.camellia.redis.proxy.kv.core.command.CommanderConfig;
import com.netease.nim.camellia.redis.proxy.kv.core.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.kv.core.meta.KeyType;
import com.netease.nim.camellia.redis.proxy.kv.core.kv.KeyValue;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.reply.StatusReply;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * HSET key field value [field value ...]
 * <p>
 * Created by caojiajun on 2024/4/7
 */
public class HSetCommander extends Commander {

    private static final byte[] script = ("local arg1 = redis.call('exists', KEYS[1]);\n" +
            "if tonumber(arg1) == 1 then\n" +
            "\tredis.call('hset', KEYS[1], ARGV[1], ARGV[2]);\n" +
            "end\n" +
            "local arg2 = redis.call('pttl', KEYS[2]);\n" +
            "if tonumber(arg2) > 0 then\n" +
            "\tredis.call('psetex', KEYS[2], arg2, ARGV[2]);\n" +
            "end").getBytes(StandardCharsets.UTF_8);
    private static final byte[] two = "2".getBytes(StandardCharsets.UTF_8);

    public HSetCommander(CommanderConfig commanderConfig) {
        super(commanderConfig);
    }

    @Override
    public RedisCommand redisCommand() {
        return RedisCommand.HSET;
    }

    @Override
    protected boolean parse(Command command) {
        byte[][] objects = command.getObjects();
        if (objects.length < 4) {
            return false;
        }
        return objects.length % 2 == 0;
    }

    @Override
    protected Reply execute(Command command) {
        byte[][] objects = command.getObjects();
        byte[] key = objects[1];

        //check meta
        KeyMeta keyMeta = keyMetaServer.getKeyMeta(key, KeyType.hash, true);

        //param parse
        byte[] cacheKey = keyStruct.cacheKey(keyMeta, key);
        List<Command> commands = new ArrayList<>();
        List<KeyValue> list = new ArrayList<>();
        for (int i=2; i<objects.length; i+=2) {
            byte[] field = objects[i];
            byte[] value = objects[i + 1];
            byte[] storeKey = keyStruct.hashFieldStoreKey(keyMeta, key, field);//store-key
            KeyValue keyValue = new KeyValue(storeKey, value);
            list.add(keyValue);
            byte[] hashFieldCacheKey = keyStruct.hashFieldCacheKey(keyMeta, key, field);//cache-key
            Command cmd = new Command(new byte[][] {RedisCommand.EVAL.raw(), script, two, cacheKey, hashFieldCacheKey, field, value});
            commands.add(cmd);
        }

        //cache
        List<Reply> replies = sync(cacheRedisTemplate.sendCommand(commands));
        for (Reply reply : replies) {
            if (reply instanceof ErrorReply) {
                return reply;
            }
        }

        //store
        kvClient.batchPut(list);
        return StatusReply.OK;
    }

}
