package com.netease.nim.camellia.redis.proxy.upstream.local.storage.command;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.compact.CompactExecutor;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.KeyReadWrite;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.value.string.StringReadWrite;

/**
 * Created by caojiajun on 2025/1/3
 */
public abstract class ICommand {

    protected CompactExecutor compactExecutor;

    protected KeyReadWrite keyReadWrite;
    protected StringReadWrite stringReadWrite;

    public ICommand(CommandConfig commandConfig) {
        compactExecutor = commandConfig.getCompactExecutor();
        keyReadWrite = commandConfig.getReadWrite().getKeyReadWrite();
        stringReadWrite = commandConfig.getReadWrite().getStringReadWrite();
    }

    /**
     * redis command of commander
     * @return redis-command
     */
    public abstract RedisCommand redisCommand();

    /**
     * check param
     * @param command command
     * @return success or fail
     */
    protected abstract boolean parse(Command command);

    /**
     * for read command run to completion
     * @param slot slot
     * @param command command
     * @return reply if run-to-completion success
     */
    public Reply runToCompletion(short slot, Command command) {
        return null;
    }

    /**
     * execute command
     * @param slot slot
     * @param command command
     * @return reply
     */
    protected abstract Reply execute(short slot, Command command) throws Exception;
}
