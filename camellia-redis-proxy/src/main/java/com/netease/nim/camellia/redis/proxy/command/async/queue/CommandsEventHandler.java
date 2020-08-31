package com.netease.nim.camellia.redis.proxy.command.async.queue;

/**
 *
 * Created by caojiajun on 2020/8/27
 */
public interface CommandsEventHandler {

    CommandsEventProducer getProducer();

    CommandsEventConsumer getConsumer();
}
