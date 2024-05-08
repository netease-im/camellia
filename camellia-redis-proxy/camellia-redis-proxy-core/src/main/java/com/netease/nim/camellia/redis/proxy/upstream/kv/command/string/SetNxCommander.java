package com.netease.nim.camellia.redis.proxy.upstream.kv.command.string;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.reply.IntegerReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.Commander;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.CommanderConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.EncodeVersion;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyType;

/**
 * SETNX key value
 * <p>
 * Created by caojiajun on 2024/5/8
 */
public class SetNxCommander extends Commander {

    public SetNxCommander(CommanderConfig commanderConfig) {
        super(commanderConfig);
    }

    @Override
    public RedisCommand redisCommand() {
        return RedisCommand.SETEX;
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
        byte[] value = objects[2];

        KeyMeta keyMeta = keyMetaServer.getKeyMeta(key);
        if (keyMeta == null) {
            keyMeta = new KeyMeta(EncodeVersion.version_0, KeyType.string, System.currentTimeMillis(), -1, value);
            keyMetaServer.createOrUpdateKeyMeta(key, keyMeta);
            return IntegerReply.REPLY_1;
        }
        return IntegerReply.REPLY_0;
    }
}
