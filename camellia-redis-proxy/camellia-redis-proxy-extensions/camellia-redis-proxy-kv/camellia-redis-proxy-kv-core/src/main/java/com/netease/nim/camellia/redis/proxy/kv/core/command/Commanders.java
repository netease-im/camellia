package com.netease.nim.camellia.redis.proxy.kv.core.command;


import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.kv.core.command.db.*;
import com.netease.nim.camellia.redis.proxy.kv.core.command.hash.HGetAllCommander;
import com.netease.nim.camellia.redis.proxy.kv.core.command.hash.HGetCommander;
import com.netease.nim.camellia.redis.proxy.kv.core.command.hash.HSetCommander;
import com.netease.nim.camellia.redis.proxy.kv.core.exception.KvException;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by caojiajun on 2024/4/7
 */
public class Commanders {

    private final Map<RedisCommand, Commander> map = new HashMap<>();

    public Commanders(CommanderConfig commanderConfig) {
        initCommander(new HSetCommander(commanderConfig));
        initCommander(new HGetCommander(commanderConfig));
        initCommander(new HGetAllCommander(commanderConfig));
        initCommander(new PExpireCommander(commanderConfig));
        initCommander(new ExpireCommander(commanderConfig));
        initCommander(new DelCommander(commanderConfig));
        initCommander(new ExistsCommander(commanderConfig));
        initCommander(new UnlinkCommander(commanderConfig));
    }

    private void initCommander(Commander commander) {
        map.put(commander.redisCommand(), commander);
    }

    public Reply execute(Command command) {
        RedisCommand redisCommand = command.getRedisCommand();
        Commander commander = map.get(redisCommand);
        if (commander == null) {
            return ErrorReply.NOT_SUPPORT;
        }
        try {
            if (!commander.parse(command)) {
                return ErrorReply.argNumWrong(redisCommand);
            }
            return commander.execute(command);
        } catch (KvException | IllegalArgumentException e) {
            ErrorLogCollector.collect(Commanders.class, redisCommand + " execute error", e);
            String message = e.getMessage();
            if (message != null) {
                if (message.startsWith("ERR")) {
                    return new ErrorReply(e.getMessage());
                } else {
                    return new ErrorReply("ERR " + e.getMessage());
                }
            } else {
                return ErrorReply.SYNTAX_ERROR;
            }
        } catch (Throwable e) {
            ErrorLogCollector.collect(Commanders.class, redisCommand + " execute error", e);
            return new ErrorReply("ERR command execute error");
        }
    }
}
