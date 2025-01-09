package com.netease.nim.camellia.redis.proxy.upstream.local.storage.command;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.compact.CompactExecutor;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.enums.FlushResult;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.KeyReadWrite;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.value.string.StringReadWrite;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.wal.WalGroup;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * Created by caojiajun on 2025/1/3
 */
public abstract class CommandOnLocalStorage {

    protected WalGroup walGroup;

    protected CompactExecutor compactExecutor;

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

    /**
     * check and flush after write
     * @param slot slot
     * @throws IOException exception
     */
    protected void afterWrite(short slot) throws IOException {
        if (keyReadWrite.needFlush(slot) || stringReadWrite.needFlush(slot)) {
            //key flush prepare
            keyReadWrite.flushPrepare(slot);
            //flush string value
            CompletableFuture<FlushResult> future1 = stringReadWrite.flush(slot);
            //flush key
            future1.thenAccept(result -> keyReadWrite.flush(slot));
        }
        compactExecutor.compact(slot);
    }
}
