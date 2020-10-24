package com.netease.nim.camellia.redis.proxy.command.async.queue.none;

import com.netease.nim.camellia.redis.proxy.command.async.AsyncCamelliaRedisTemplateChooser;
import com.netease.nim.camellia.redis.proxy.command.async.CommandInvokeConfig;
import com.netease.nim.camellia.redis.proxy.command.async.queue.CommandsEventConsumer;
import com.netease.nim.camellia.redis.proxy.command.async.queue.CommandsEventHandler;
import com.netease.nim.camellia.redis.proxy.command.async.queue.CommandsEventProducer;

/**
 *
 * Created by caojiajun on 2020/8/27
 */
public class NoneQueueCommandsEventHandler implements CommandsEventHandler {

    private final NoneQueueCommandsEventProducerConsumer instance;

    public NoneQueueCommandsEventHandler(AsyncCamelliaRedisTemplateChooser chooser, CommandInvokeConfig commandInvokeConfig) {
        instance = new NoneQueueCommandsEventProducerConsumer(chooser, commandInvokeConfig);
    }

    @Override
    public CommandsEventProducer getProducer() {
        return instance;
    }

    @Override
    public CommandsEventConsumer getConsumer() {
        return instance;
    }
}
