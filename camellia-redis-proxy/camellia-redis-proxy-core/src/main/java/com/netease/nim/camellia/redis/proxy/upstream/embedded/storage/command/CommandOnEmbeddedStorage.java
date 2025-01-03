package com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.command;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.key.KeyReadWrite;
import com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.value.string.StringReadWrite;
import com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.wal.WalGroup;

/**
 * Created by caojiajun on 2025/1/3
 */
public abstract class CommandOnEmbeddedStorage {

    protected WalGroup walGroup;

    protected KeyReadWrite keyReadWrite;
    protected StringReadWrite stringReadWrite;

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
     * execute command
     * @param slot slot
     * @param command command
     * @return reply
     */
    protected abstract Reply execute(short slot, Command command) throws Exception;
}
