package com.netease.nim.camellia.redis.proxy.command.async;

/**
 * Created by caojiajun on 2020/7/7.
 */
public class CommandWrapperEvent {
    private CommandWrapper commandWrapper;

    public CommandWrapper getCommandWrapper() {
        return commandWrapper;
    }

    public void setCommandWrapper(CommandWrapper commandWrapper) {
        this.commandWrapper = commandWrapper;
    }
}
