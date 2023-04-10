package com.netease.nim.camellia.redis.proxy.netty;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import io.netty.channel.EventLoop;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Created by caojiajun on 2023/4/8
 */
public class CommandPackRecycler {

    private final EventLoop eventLoop;
    private CommandPack commandPack;

    private static boolean recycleEnable;
    static {
        reloadConf();
        ProxyDynamicConf.registerCallback(CommandPackRecycler::reloadConf);
    }
    private static void reloadConf() {
        recycleEnable = ProxyDynamicConf.getBoolean("command.pack.recycle.enable", true);
    }

    public CommandPackRecycler(EventLoop eventLoop) {
        this.eventLoop = eventLoop;
    }

    public CommandPack newInstance(List<Command> commands, List<CompletableFuture<Reply>> completableFutureList, long startTime) {
        if (!recycleEnable) {
            return new CommandPack(commands, completableFutureList, startTime);
        }
        if (eventLoop.inEventLoop()) {
            CommandPack pack;
            if (commandPack != null) {
                pack = commandPack;
                commandPack = null;
                pack.setCommands(commands);
                pack.setCompletableFutureList(completableFutureList);
                pack.setStartTime(startTime);
            } else {
                pack = new CommandPack(commands, completableFutureList, startTime);
            }
            return pack;
        } else {
            return new CommandPack(commands, completableFutureList, startTime);
        }
    }

    //only invoke this method inEventLoop
    public void recycle(CommandPack commandPack) {
        if (!recycleEnable) return;
        if (this.commandPack == null) {
            commandPack.setCommands(null);
            commandPack.setCompletableFutureList(null);
            commandPack.setStartTime(0);
            this.commandPack = commandPack;
        }
    }
}
