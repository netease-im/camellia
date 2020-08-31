package com.netease.nim.camellia.redis.proxy.command.async.queue;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.netty.ChannelInfo;

import java.util.List;

/**
 *
 * Created by caojiajun on 2020/8/25
 */
public class CommandsEvent {
    private ChannelInfo channelInfo;
    private List<Command> commands;

    public CommandsEvent() {
    }

    public CommandsEvent(ChannelInfo channelInfo, List<Command> commands) {
        this.channelInfo = channelInfo;
        this.commands = commands;
    }

    public ChannelInfo getChannelInfo() {
        return channelInfo;
    }

    public void setChannelInfo(ChannelInfo channelInfo) {
        this.channelInfo = channelInfo;
    }

    public List<Command> getCommands() {
        return commands;
    }

    public void setCommands(List<Command> commands) {
        this.commands = commands;
    }
}
