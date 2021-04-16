package com.netease.nim.camellia.redis.proxy.command.async;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 *
 * Created by caojiajun on 2019/12/19.
 */
public class CommandFlusher {

    private final Map<AsyncClient, List<Command>> commandMap = new HashMap<>();
    private final Map<AsyncClient, List<CompletableFuture<Reply>>> futureMap = new HashMap<>();

    private final int initializerSize;

    public CommandFlusher(int initializerSize) {
        this.initializerSize = initializerSize;
    }

    public CommandFlusher() {
        this(10);
    }

    public void sendCommand(AsyncClient client, Command command, CompletableFuture<Reply> future) {
        List<Command> commands = commandMap.get(client);
        if (commands == null) {
            commands = commandMap.computeIfAbsent(client, k -> new ArrayList<>(initializerSize));
        }
        commands.add(command);
        List<CompletableFuture<Reply>> futures = futureMap.get(client);
        if (futures == null) {
            futures = futureMap.computeIfAbsent(client, k -> new ArrayList<>(initializerSize));
        }
        futures.add(future);
    }

    public CompletableFuture<Reply> sendCommand(AsyncClient client, Command command) {
        CompletableFuture<Reply> future = new CompletableFuture<>();
        sendCommand(client, command, future);
        return future;
    }

    public void flush() {
        for (Map.Entry<AsyncClient, List<Command>> entry : commandMap.entrySet()) {
            AsyncClient client = entry.getKey();
            List<Command> commands = entry.getValue();
            List<CompletableFuture<Reply>> futureList = futureMap.get(client);
            if (client == null) {
                for (CompletableFuture<Reply> future : futureList) {
                    future.complete(ErrorReply.NOT_AVAILABLE);
                    ErrorLogCollector.collect(CommandFlusher.class, "AsyncClient is null, return NOT_AVAILABLE");
                }
            } else {
                client.sendCommand(commands, futureList);
            }
        }
    }

    public void clear() {
        commandMap.clear();
        futureMap.clear();
    }
}
