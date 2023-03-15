package com.netease.nim.camellia.redis.proxy.upstream;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnection;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Created by caojiajun on 2023/2/17
 */
public class RedisConnectionCommandFlusher {
    private final Map<RedisConnection, List<Command>> commandMap = new HashMap<>();
    private final Map<RedisConnection, List<CompletableFuture<Reply>>> futureMap = new HashMap<>();

    private int initializerSize;

    public RedisConnectionCommandFlusher(int initializerSize) {
        this.initializerSize = initializerSize;
    }

    public RedisConnectionCommandFlusher() {
        this(10);
    }

    public int getInitializerSize() {
        return initializerSize;
    }

    public void updateInitializerSize(int initializerSize) {
        this.initializerSize = initializerSize;
    }

    public void sendCommand(RedisConnection connection, Command command, CompletableFuture<Reply> future) {
        List<Command> commands = commandMap.get(connection);
        if (commands == null) {
            commands = commandMap.computeIfAbsent(connection, k -> new ArrayList<>(initializerSize));
        }
        commands.add(command);
        List<CompletableFuture<Reply>> futures = futureMap.get(connection);
        if (futures == null) {
            futures = futureMap.computeIfAbsent(connection, k -> new ArrayList<>(initializerSize));
        }
        futures.add(future);
    }

    public CompletableFuture<Reply> sendCommand(RedisConnection connection, Command command) {
        CompletableFuture<Reply> future = new CompletableFuture<>();
        sendCommand(connection, command, future);
        return future;
    }

    public void flush() {
        for (Map.Entry<RedisConnection, List<Command>> entry : commandMap.entrySet()) {
            RedisConnection connection = entry.getKey();
            List<Command> commands = entry.getValue();
            List<CompletableFuture<Reply>> futureList = futureMap.get(connection);
            if (connection == null) {
                for (CompletableFuture<Reply> future : futureList) {
                    future.complete(ErrorReply.UPSTREAM_CONNECTION_NOT_AVAILABLE);
                    ErrorLogCollector.collect(UpstreamClientCommandFlusher.class, "RedisConnection is null, return NOT_AVAILABLE");
                }
            } else {
                connection.sendCommand(commands, futureList);
            }
        }
    }

    public void clear() {
        commandMap.clear();
        futureMap.clear();
    }
}
