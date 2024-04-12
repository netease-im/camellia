package com.netease.nim.camellia.redis.proxy.kv.core.command.string;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.kv.core.command.Commander;
import com.netease.nim.camellia.redis.proxy.kv.core.command.CommanderConfig;
import com.netease.nim.camellia.redis.proxy.reply.Reply;

/**
 * Created by caojiajun on 2024/4/11
 */
public class PSetExCommander extends Commander {

    public PSetExCommander(CommanderConfig commanderConfig) {
        super(commanderConfig);
    }

    @Override
    public RedisCommand redisCommand() {
        return null;
    }

    @Override
    protected boolean parse(Command command) {
        return false;
    }

    @Override
    protected Reply execute(Command command) {
        return null;
    }
}
