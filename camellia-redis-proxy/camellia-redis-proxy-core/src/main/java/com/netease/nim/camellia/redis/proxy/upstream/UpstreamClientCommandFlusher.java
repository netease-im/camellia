package com.netease.nim.camellia.redis.proxy.upstream;

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
public class UpstreamClientCommandFlusher {

    private final Map<IUpstreamClient, List<Command>> commandMap = new HashMap<>();
    private final Map<IUpstreamClient, List<CompletableFuture<Reply>>> futureMap = new HashMap<>();

    private int initializerSize;
    private final int db;

    public UpstreamClientCommandFlusher(int db, int initializerSize) {
        this.initializerSize = initializerSize;
        this.db = db;
    }

    public UpstreamClientCommandFlusher() {
        this(-1, 10);
    }

    public int getInitializerSize() {
        return initializerSize;
    }

    public void updateInitializerSize(int initializerSize) {
        this.initializerSize = initializerSize;
    }

    public void sendCommand(IUpstreamClient client, Command command, CompletableFuture<Reply> future) {
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

    public CompletableFuture<Reply> sendCommand(IUpstreamClient client, Command command) {
        CompletableFuture<Reply> future = new CompletableFuture<>();
        sendCommand(client, command, future);
        return future;
    }

    public void flush() {
        for (Map.Entry<IUpstreamClient, List<Command>> entry : commandMap.entrySet()) {
            IUpstreamClient client = entry.getKey();
            List<Command> commands = entry.getValue();
            List<CompletableFuture<Reply>> futureList = futureMap.get(client);
            if (client == null) {
                for (CompletableFuture<Reply> future : futureList) {
                    future.complete(ErrorReply.NOT_AVAILABLE);
                    ErrorLogCollector.collect(UpstreamClientCommandFlusher.class, "IUpstreamClient is null, return NOT_AVAILABLE");
                }
            } else {
                client.sendCommand(db, commands, futureList);
            }
        }
    }

    public void clear() {
        commandMap.clear();
        futureMap.clear();
    }
}
