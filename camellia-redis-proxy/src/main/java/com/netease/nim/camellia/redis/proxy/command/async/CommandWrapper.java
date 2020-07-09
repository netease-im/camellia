package com.netease.nim.camellia.redis.proxy.command.async;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.reply.Reply;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Created by caojiajun on 2020/7/7.
 */
public class CommandWrapper {
    private List<Command> commands;
    private List<CompletableFuture<Reply>> completableFutureList;

    public CommandWrapper(List<Command> commands, List<CompletableFuture<Reply>> completableFutureList) {
        this.commands = commands;
        this.completableFutureList = completableFutureList;
    }

    public List<Command> getCommands() {
        return commands;
    }

    public void setCommands(List<Command> commands) {
        this.commands = commands;
    }

    public List<CompletableFuture<Reply>> getCompletableFutureList() {
        return completableFutureList;
    }

    public void setCompletableFutureList(List<CompletableFuture<Reply>> completableFutureList) {
        this.completableFutureList = completableFutureList;
    }
}
