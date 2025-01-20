package com.netease.nim.camellia.redis.proxy.upstream.local.storage.command;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.Commanders;
import com.netease.nim.camellia.redis.proxy.upstream.kv.exception.KvException;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.command.db.*;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.command.string.*;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.compact.CompactExecutor;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.flush.FlushResult;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.Key;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.KeyInfo;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.KeyReadWrite;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.value.string.StringReadWrite;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.wal.SlotWalOffset;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.wal.Wal;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Created by caojiajun on 2025/1/10
 */
public class Commands {

    private final Map<RedisCommand, ICommand> map = new HashMap<>();

    protected CompactExecutor compactExecutor;

    protected KeyReadWrite keyReadWrite;
    protected StringReadWrite stringReadWrite;

    protected Wal wal;

    public Commands(CommandConfig commandConfig) {
        compactExecutor = commandConfig.getCompactExecutor();
        keyReadWrite = commandConfig.getReadWrite().getKeyReadWrite();
        stringReadWrite = commandConfig.getReadWrite().getStringReadWrite();
        wal = commandConfig.getWal();

        //db
        initCommand(new MemFlushCommand(commandConfig));
        initCommand(new PExpireCommand(commandConfig));
        initCommand(new ExpireCommand(commandConfig));
        initCommand(new DelCommand(commandConfig));
        initCommand(new ExistsCommand(commandConfig));
        initCommand(new UnlinkCommand(commandConfig));
        initCommand(new TTLCommand(commandConfig));
        initCommand(new PTTLCommand(commandConfig));
        initCommand(new TypeCommand(commandConfig));
        initCommand(new ExpireAtCommand(commandConfig));
        initCommand(new PExpireAtCommand(commandConfig));
        initCommand(new ExpireTimeCommand(commandConfig));
        initCommand(new PExpireTimeCommand(commandConfig));

        //string
        initCommand(new GetCommand(commandConfig));
        initCommand(new PSetExCommand(commandConfig));
        initCommand(new SetExCommand(commandConfig));
        initCommand(new SetCommand(commandConfig));
        initCommand(new SetNxCommand(commandConfig));
        initCommand(new StrLenCommand(commandConfig));
    }

    private void initCommand(ICommand command) {
        map.put(command.redisCommand(), command);
    }

    public ICommand getCommandInvoker(RedisCommand redisCommand) {
        return map.get(redisCommand);
    }

    public boolean parse(ICommand invoker, Command command) {
        return invoker.parse(command);
    }

    public Reply execute(ICommand invoker, short slot, Command command) {
        try {
            try {
                return invoker.execute(slot, command);
            } finally {
                if (invoker.redisCommand().getType() == RedisCommand.Type.WRITE) {
                    afterWrite(slot);
                }
            }
        } catch (Throwable e) {
            return onException(command.getRedisCommand(), e);
        }
    }

    private void afterWrite(short slot) throws IOException {
        //compact
        compactExecutor.compact(slot);
        //check need flush
        if (keyReadWrite.needFlush(slot) || stringReadWrite.needFlush(slot)) {
            //获取slot当前wal写到哪里了
            SlotWalOffset slotWalOffset = wal.getSlotWalOffsetEnd(slot);
            //key flush prepare
            Map<Key, KeyInfo> keyMap = keyReadWrite.flushPrepare(slot);
            //flush string value
            CompletableFuture<FlushResult> future1 = stringReadWrite.flush(slot, keyMap);
            future1.thenAccept(flushResult1 -> {
                //flush key
                CompletableFuture<FlushResult> future2 = keyReadWrite.flush(slot);
                //flush wal，表示这个offset之前的关于指定slot的日志条目都无效了
                future2.thenAccept(flushResult2 -> wal.flush(slot, slotWalOffset));
            });
        }
    }

    private Reply onException(RedisCommand redisCommand, Throwable e) {
        if (e instanceof KvException || e instanceof IllegalArgumentException) {
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
        }
        ErrorLogCollector.collect(Commanders.class, redisCommand + " execute error", e);
        return new ErrorReply("ERR command execute error");
    }

}
