package com.netease.nim.camellia.redis.proxy.upstream.kv.command.set;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.CommanderConfig;

/**
 * SRANDMEMBER key [count]
 * <p>
 * Created by caojiajun on 2024/8/5
 */
public class SRandomMemberCommander extends Set0Commander {

    public SRandomMemberCommander(CommanderConfig commanderConfig) {
        super(commanderConfig);
    }

    @Override
    public RedisCommand redisCommand() {
        return RedisCommand.SRANDMEMBER;
    }

    @Override
    protected boolean parse(Command command) {
        byte[][] objects = command.getObjects();
        return objects.length == 2 || objects.length == 3;
    }

    @Override
    protected Reply execute(Command command) {
        return null;
    }
}
