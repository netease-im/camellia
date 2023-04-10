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
    private List<Command> commands;
    private List<CompletableFuture<Reply>> completableFutureList;
    private long startTime;

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

    public void setCommands(List<Command> commands) {
        this.commands = commands;
    }

    public void setCompletableFutureList(List<CompletableFuture<Reply>> completableFutureList) {
        this.completableFutureList = completableFutureList;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }
}
