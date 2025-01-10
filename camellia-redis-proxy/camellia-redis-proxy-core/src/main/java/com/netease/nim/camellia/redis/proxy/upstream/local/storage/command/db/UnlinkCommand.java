package com.netease.nim.camellia.redis.proxy.upstream.local.storage.command.db;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.command.CommandConfig;

/**
 * UNLINK key
 * <p>
 * Created by caojiajun on 2025/1/10
 */
public class UnlinkCommand extends DelCommand {

    public UnlinkCommand(CommandConfig commandConfig) {
        super(commandConfig);
    }

    @Override
    public RedisCommand redisCommand() {
        return RedisCommand.UNLINK;
    }

    @Override
    protected boolean parse(Command command) {
        return super.parse(command);
    }

    @Override
    protected Reply execute(short slot, Command command) throws Exception {
        return super.execute(slot, command);
    }
}
