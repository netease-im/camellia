package com.netease.nim.camellia.redis.proxy.upstream.kv.command.set;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.Commander;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.CommanderConfig;

/**
 * SISMEMBER key member
 * <p>
 * Created by caojiajun on 2024/8/5
 */
public class SIsMemberCommander extends Commander {

    public SIsMemberCommander(CommanderConfig commanderConfig) {
        super(commanderConfig);
    }

    @Override
    public RedisCommand redisCommand() {
        return RedisCommand.SISMEMBER;
    }

    @Override
    protected boolean parse(Command command) {
        return command.getObjects().length == 3;
    }

    @Override
    protected Reply execute(Command command) {
        return null;
    }
}
