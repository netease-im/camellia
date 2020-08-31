package com.netease.nim.camellia.redis.proxy.command.async.queue.disruptor;

import com.lmax.disruptor.EventFactory;
import com.netease.nim.camellia.redis.proxy.command.async.queue.CommandsEvent;

/**
 *
 * Created by caojiajun on 2020/8/25
 */
public class DisruptorCommandsEventFactory implements EventFactory<CommandsEvent> {

    @Override
    public CommandsEvent newInstance() {
        return new CommandsEvent();
    }
}
