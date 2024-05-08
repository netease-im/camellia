package com.netease.nim.camellia.redis.proxy.upstream.kv.command;


import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.db.*;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.hash.*;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.string.*;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.zset.*;
import com.netease.nim.camellia.redis.proxy.upstream.kv.exception.KvException;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by caojiajun on 2024/4/7
 */
public class Commanders {

    private final Map<RedisCommand, Commander> map = new HashMap<>();

    public Commanders(CommanderConfig commanderConfig) {
        //db
        initCommander(new PExpireCommander(commanderConfig));
        initCommander(new ExpireCommander(commanderConfig));
        initCommander(new DelCommander(commanderConfig));
        initCommander(new ExistsCommander(commanderConfig));
        initCommander(new UnlinkCommander(commanderConfig));
        initCommander(new KvCommander(commanderConfig));
        initCommander(new TTLCommander(commanderConfig));
        initCommander(new PTTLCommander(commanderConfig));
        initCommander(new TypeCommander(commanderConfig));
        initCommander(new ExpireAtCommander(commanderConfig));
        initCommander(new PExpireAtCommander(commanderConfig));
        initCommander(new ExpireTimeCommander(commanderConfig));
        initCommander(new PExpireTimeCommander(commanderConfig));

        //string
        initCommander(new GetCommander(commanderConfig));
        initCommander(new PSetExCommander(commanderConfig));
        initCommander(new SetExCommander(commanderConfig));
        initCommander(new SetCommander(commanderConfig));
        initCommander(new SetNxCommander(commanderConfig));

        //hash
        initCommander(new HSetCommander(commanderConfig));
        initCommander(new HMSetCommander(commanderConfig));
        initCommander(new HGetCommander(commanderConfig));
        initCommander(new HGetAllCommander(commanderConfig));
        initCommander(new HDelCommander(commanderConfig));
        initCommander(new HLenCommander(commanderConfig));
        initCommander(new HMGetCommander(commanderConfig));

        //zset
        initCommander(new ZAddCommander(commanderConfig));
        initCommander(new ZCardCommander(commanderConfig));
        initCommander(new ZRangeCommander(commanderConfig));
        initCommander(new ZRangeByScoreCommander(commanderConfig));
        initCommander(new ZRangeByLexCommander(commanderConfig));
        initCommander(new ZRevRangeCommander(commanderConfig));
        initCommander(new ZRevRangeByScoreCommander(commanderConfig));
        initCommander(new ZRevRangeByLexCommander(commanderConfig));
        initCommander(new ZRemRangeByRankCommander(commanderConfig));
        initCommander(new ZRemRangeByScoreCommander(commanderConfig));
        initCommander(new ZRemRangeByLexCommander(commanderConfig));

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
