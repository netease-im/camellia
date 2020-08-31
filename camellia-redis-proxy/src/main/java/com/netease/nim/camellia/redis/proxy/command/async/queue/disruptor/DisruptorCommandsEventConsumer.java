package com.netease.nim.camellia.redis.proxy.command.async.queue.disruptor;


import com.lmax.disruptor.EventHandler;
import com.netease.nim.camellia.redis.proxy.command.async.AsyncCamelliaRedisTemplateChooser;
import com.netease.nim.camellia.redis.proxy.command.async.CommandInterceptor;
import com.netease.nim.camellia.redis.proxy.command.async.queue.AbstractCommandsEventConsumer;
import com.netease.nim.camellia.redis.proxy.command.async.queue.CommandsEvent;

/**
 *
 * Created by caojiajun on 2020/8/25
 */
public class DisruptorCommandsEventConsumer extends AbstractCommandsEventConsumer implements EventHandler<CommandsEvent> {

    public DisruptorCommandsEventConsumer(AsyncCamelliaRedisTemplateChooser chooser, CommandInterceptor commandInterceptor, int commandPipelineFlushThreshold,
                                          boolean commandSpendTimeMonitorEnable, long slowCommandThresholdMillisTime) {
        super(chooser, commandInterceptor, commandPipelineFlushThreshold, commandSpendTimeMonitorEnable, slowCommandThresholdMillisTime);
    }

    @Override
    public void onEvent(CommandsEvent commandsEvent, long sequence, boolean endOfBatch) {
        super.onEvent(commandsEvent, endOfBatch);
    }
}
