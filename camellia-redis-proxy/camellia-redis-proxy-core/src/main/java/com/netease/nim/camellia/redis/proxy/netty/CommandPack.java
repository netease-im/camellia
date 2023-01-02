package com.netease.nim.camellia.redis.proxy.netty;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.reply.Reply;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 *
 * Created by caojiajun on 2020/8/19
 */
public class CommandPack {
    private final List<Command> commands;
    private final List<CompletableFuture<Reply>> completableFutureList;
    private final long startTime;

    public CommandPack(List<Command> commands, List<CompletableFuture<Reply>> completableFutureList, long startTime) {
        this.commands = commands;
        this.completableFutureList = completableFutureList;
        this.startTime = startTime;
    }

    public List<Command> getCommands() {
        return commands;
    }

    public List<CompletableFuture<Reply>> getCompletableFutureList() {
        return completableFutureList;
    }

    public long getStartTime() {
        return startTime;
    }
}
