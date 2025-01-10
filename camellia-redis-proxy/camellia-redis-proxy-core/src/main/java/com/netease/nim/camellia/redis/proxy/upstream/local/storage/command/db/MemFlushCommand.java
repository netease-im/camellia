package com.netease.nim.camellia.redis.proxy.upstream.local.storage.command.db;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.reply.StatusReply;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.command.CommandConfig;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.command.ICommand;

/**
 * Created by caojiajun on 2025/1/10
 */
public class MemFlushCommand extends ICommand {

    public static final Command command = new Command(new byte[][]{RedisCommand.MEMFLUSH.raw()});

    public MemFlushCommand(CommandConfig commandConfig) {
        super(commandConfig);
    }

    @Override
    public RedisCommand redisCommand() {
        return RedisCommand.MEMFLUSH;
    }

    @Override
    protected boolean parse(Command command) {
        return true;
    }

    @Override
    protected Reply execute(short slot, Command command) throws Exception {
        return StatusReply.OK;
    }
}
