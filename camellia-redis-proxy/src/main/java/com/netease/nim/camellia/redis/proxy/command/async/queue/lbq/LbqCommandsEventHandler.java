package com.netease.nim.camellia.redis.proxy.command.async.queue.lbq;

import com.netease.nim.camellia.redis.proxy.command.async.AsyncCamelliaRedisTemplateChooser;
import com.netease.nim.camellia.redis.proxy.command.async.CommandInvokeConfig;
import com.netease.nim.camellia.redis.proxy.command.async.queue.CommandsEvent;
import com.netease.nim.camellia.redis.proxy.command.async.queue.CommandsEventConsumer;
import com.netease.nim.camellia.redis.proxy.command.async.queue.CommandsEventHandler;
import com.netease.nim.camellia.redis.proxy.command.async.queue.CommandsEventProducer;

import java.util.concurrent.LinkedBlockingQueue;

/**
 *
 * Created by caojiajun on 2020/8/27
 */
public class LbqCommandsEventHandler implements CommandsEventHandler {

    private final LbqCommandsEventConsumer consumer;
    private final LbqCommandsEventProducer producer;

    public LbqCommandsEventHandler(AsyncCamelliaRedisTemplateChooser chooser, CommandInvokeConfig commandInvokeConfig) {
        LinkedBlockingQueue<CommandsEvent> queue = new LinkedBlockingQueue<>(1024 * 128);
        consumer = new LbqCommandsEventConsumer(queue, chooser, commandInvokeConfig);
        consumer.start();
        producer = new LbqCommandsEventProducer(queue);
    }

    @Override
    public CommandsEventProducer getProducer() {
        return producer;
    }

    @Override
    public CommandsEventConsumer getConsumer() {
        return consumer;
    }
}
