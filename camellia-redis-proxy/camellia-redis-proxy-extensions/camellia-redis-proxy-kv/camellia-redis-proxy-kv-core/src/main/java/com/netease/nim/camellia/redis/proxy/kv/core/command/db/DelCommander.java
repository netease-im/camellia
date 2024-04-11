package com.netease.nim.camellia.redis.proxy.kv.core.command.db;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.kv.core.command.Commander;
import com.netease.nim.camellia.redis.proxy.kv.core.command.CommanderConfig;
import com.netease.nim.camellia.redis.proxy.reply.IntegerReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;

/**
 * Created by caojiajun on 2024/4/8
 */
public class DelCommander extends Commander {

    public DelCommander(CommanderConfig commanderConfig) {
        super(commanderConfig);
    }

    @Override
    public RedisCommand redisCommand() {
        return RedisCommand.DEL;
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
        int ret = keyMetaServer.existsKeyMeta(key);
        if (ret == 0) {
            return IntegerReply.REPLY_0;
        } else if (ret == 1) {
            return IntegerReply.REPLY_1;
        } else {
            return new IntegerReply((long) ret);
        }
    }
}
