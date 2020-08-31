package com.netease.nim.camellia.redis.proxy.command.async.queue.lbq;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.command.async.queue.CommandsEvent;
import com.netease.nim.camellia.redis.proxy.command.async.queue.CommandsEventProducer;
import com.netease.nim.camellia.redis.proxy.netty.ChannelInfo;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

/**
 *
 * Created by caojiajun on 2020/8/27
 */
public class LbqCommandsEventProducer implements CommandsEventProducer {

    private final LinkedBlockingQueue<CommandsEvent> queue;

    public LbqCommandsEventProducer(LinkedBlockingQueue<CommandsEvent> queue) {
        this.queue = queue;
    }

    @Override
    public boolean publishEvent(ChannelInfo channelInfo, List<Command> commands) {
        return queue.offer(new CommandsEvent(channelInfo, commands));
    }
}
