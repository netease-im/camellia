package com.netease.nim.camellia.redis.proxy.command.async.queue.none;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.command.async.AsyncCamelliaRedisTemplateChooser;
import com.netease.nim.camellia.redis.proxy.command.async.CommandInterceptor;
import com.netease.nim.camellia.redis.proxy.command.async.queue.AbstractCommandsEventConsumer;
import com.netease.nim.camellia.redis.proxy.command.async.queue.CommandsEvent;
import com.netease.nim.camellia.redis.proxy.command.async.queue.CommandsEventProducer;
import com.netease.nim.camellia.redis.proxy.netty.ChannelInfo;

import java.util.List;

/**
 *
 * Created by caojiajun on 2020/8/27
 */
public class NoneQueueCommandsEventProducerConsumer extends AbstractCommandsEventConsumer implements CommandsEventProducer {

    public NoneQueueCommandsEventProducerConsumer(AsyncCamelliaRedisTemplateChooser chooser, CommandInterceptor commandInterceptor, int commandPipelineFlushThreshold, boolean commandSpendTimeMonitorEnable, long slowCommandThresholdMillisTime) {
        super(chooser, commandInterceptor, commandPipelineFlushThreshold, commandSpendTimeMonitorEnable, slowCommandThresholdMillisTime);
    }

    @Override
    public boolean publishEvent(ChannelInfo channelInfo, List<Command> commands) {
        onEvent(new CommandsEvent(channelInfo, commands), true);
        return true;
    }
}
