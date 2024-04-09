package com.netease.nim.camellia.redis.proxy.kv.core;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.kv.core.command.CommanderConfig;
import com.netease.nim.camellia.redis.proxy.kv.core.command.Commanders;
import com.netease.nim.camellia.redis.proxy.reply.BulkReply;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.reply.StatusReply;
import com.netease.nim.camellia.redis.proxy.upstream.IUpstreamClientTemplate;
import com.netease.nim.camellia.tools.executor.CamelliaHashedExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Created by caojiajun on 2024/4/8
 */
public class UpstreamKVClientTemplate implements IUpstreamClientTemplate {

    private final Commanders commanders;
    private final CamelliaHashedExecutor executor;

    public UpstreamKVClientTemplate(CamelliaHashedExecutor executor, CommanderConfig commanderConfig) {
        this.executor = executor;
        this.commanders = new Commanders(commanderConfig);
    }

    @Override
    public List<CompletableFuture<Reply>> sendCommand(int db, List<Command> commands) {
        List<CompletableFuture<Reply>> futureList = new ArrayList<>(commands.size());
        for (Command command : commands) {
            CompletableFuture<Reply> future = new CompletableFuture<>();
            futureList.add(future);
            RedisCommand redisCommand = command.getRedisCommand();
            if (redisCommand.getCommandKeyType() == RedisCommand.CommandKeyType.None) {
                sendNoneKeyCommand(redisCommand, command, future);
                continue;
            }
            List<byte[]> keys = command.getKeys();
            if (keys.size() == 1) {
                byte[] key = keys.get(0);
                sendCommand(key, command, future);
            } else {
                future.complete(ErrorReply.NOT_SUPPORT);
            }
        }
        return futureList;
    }

    private void sendNoneKeyCommand(RedisCommand redisCommand, Command command, CompletableFuture<Reply> future) {
        if (redisCommand == RedisCommand.PING) {
            future.complete(StatusReply.PONG);
        } else if (redisCommand == RedisCommand.ECHO) {
            byte[][] objects = command.getObjects();
            if (objects.length == 2) {
                future.complete(new BulkReply(objects[1]));
            } else {
                future.complete(ErrorReply.argNumWrong(command.getRedisCommand()));
            }
        } else {
            future.complete(ErrorReply.NOT_SUPPORT);
        }
    }

    private void sendCommand(byte[] key, Command command, CompletableFuture<Reply> future) {
        try {
            executor.submit(key, () -> {
                try {
                    Reply reply = commanders.execute(command);
                    future.complete(reply);
                } catch (Exception e) {
                    future.complete(ErrorReply.NOT_AVAILABLE);
                }
            });
        } catch (Exception e) {
            future.complete(ErrorReply.TOO_BUSY);
        }
    }

    @Override
    public boolean isMultiDBSupport() {
        return false;
    }

    @Override
    public void shutdown() {
    }
}
