package com.netease.nim.camellia.redis.proxy.command.async.queue.disruptor;

import com.lmax.disruptor.EventTranslatorTwoArg;
import com.lmax.disruptor.RingBuffer;
import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.command.async.queue.CommandsEvent;
import com.netease.nim.camellia.redis.proxy.command.async.queue.CommandsEventProducer;
import com.netease.nim.camellia.redis.proxy.netty.ChannelInfo;

import java.util.List;

/**
 *
 * Created by caojiajun on 2020/8/25
 */
public class DisruptorCommandsEventProducer implements CommandsEventProducer {

    private final RingBuffer<CommandsEvent> ringBuffer;

    public DisruptorCommandsEventProducer(RingBuffer<CommandsEvent> ringBuffer) {
        this.ringBuffer = ringBuffer;
    }

    private static final EventTranslatorTwoArg<CommandsEvent, ChannelInfo, List<Command>> TRANSLATOR = (commandsEvent, l, channelInfo, commands) -> {
        commandsEvent.setChannelInfo(channelInfo);
        commandsEvent.setCommands(commands);
    };

    @Override
    public boolean publishEvent(ChannelInfo channelInfo, List<Command> commands) {
        ringBuffer.publishEvent(TRANSLATOR, channelInfo, commands);
        return true;
    }
}
