package com.netease.nim.camellia.redis.proxy.upstream.kv.command.db;

import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.CommanderConfig;


/**
 * DEL key
 * <p>
 * Created by caojiajun on 2024/4/8
 */
public class DelCommander extends Del0Commander {

    public DelCommander(CommanderConfig commanderConfig) {
        super(commanderConfig);
    }

    @Override
    public RedisCommand redisCommand() {
        return RedisCommand.DEL;
    }
}
