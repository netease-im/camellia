package com.netease.nim.camellia.redis.proxy.plugin;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.command.CommandContext;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.reply.Reply;

/**
 * Created by caojiajun on 2022/9/14
 */
public class ProxyReply {
    private final Command command;
    private final RedisCommand redisCommand;
    private final CommandContext commandContext;
    private final Reply reply;
    private final boolean fromPlugin;

    public ProxyReply(Command command, Reply reply, boolean fromPlugin) {
        this.command = command;
        this.commandContext = command.getCommandContext();
        this.redisCommand = command.getRedisCommand();
        this.reply = reply;
        this.fromPlugin = fromPlugin;
    }

    public ProxyReply(CommandContext commandContext, RedisCommand redisCommand, Reply reply, boolean fromPlugin) {
        this.command = null;
        this.commandContext = commandContext;
        this.redisCommand = redisCommand;
        this.reply = reply;
        this.fromPlugin = fromPlugin;
    }

    public boolean isFromPlugin() {
        return fromPlugin;
    }

    public CommandContext getCommandContext() {
        return commandContext;
    }

    public RedisCommand getRedisCommand() {
        return redisCommand;
    }

    public Command getCommand() {
        return command;
    }

    public Reply getReply() {
        return reply;
    }

}
