package com.netease.nim.camellia.redis.proxy.command.async.queue.disruptor;

import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.netease.nim.camellia.redis.exception.CamelliaRedisException;
import com.netease.nim.camellia.redis.proxy.command.async.AsyncCamelliaRedisTemplateChooser;
import com.netease.nim.camellia.redis.proxy.command.async.CommandInvokeConfig;
import com.netease.nim.camellia.redis.proxy.command.async.queue.CommandsEvent;
import com.netease.nim.camellia.redis.proxy.command.async.queue.CommandsEventConsumer;
import com.netease.nim.camellia.redis.proxy.command.async.queue.CommandsEventHandler;
import com.netease.nim.camellia.redis.proxy.command.async.queue.CommandsEventProducer;
import com.netease.nim.camellia.redis.proxy.conf.CamelliaTranspondProperties;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * Created by caojiajun on 2020/8/27
 */
public class DisruptorCommandsEventHandler implements CommandsEventHandler {

    private static final Logger logger = LoggerFactory.getLogger(DisruptorCommandsEventHandler.class);

    private final DisruptorCommandsEventConsumer consumer;
    private final DisruptorCommandsEventProducer producer;

    public DisruptorCommandsEventHandler(CamelliaTranspondProperties.RedisConfProperties.DisruptorConf disruptorConf,
                                         AsyncCamelliaRedisTemplateChooser chooser,
                                         CommandInvokeConfig commandInvokeConfig) {
        try {
            consumer = new DisruptorCommandsEventConsumer(chooser, commandInvokeConfig);

            WaitStrategy waitStrategy = new SleepingWaitStrategy();
            if (disruptorConf != null) {
                String waitStrategyClassName = disruptorConf.getWaitStrategyClassName();
                if (waitStrategyClassName != null) {
                    Class<?> clazz = Class.forName(waitStrategyClassName);
                    waitStrategy = (WaitStrategy) clazz.newInstance();
                }
            }

            DisruptorCommandsEventFactory factory = new DisruptorCommandsEventFactory();
            Disruptor<CommandsEvent> disruptor = new Disruptor<>(factory, 1024*128,
                    new DefaultThreadFactory("disruptor-commands-event"), ProducerType.SINGLE, waitStrategy);
            logger.info("Disruptor init success, WaitStrategy = {}", waitStrategy.getClass().getName());
            disruptor.handleEventsWith(consumer);
            disruptor.start();
            RingBuffer<CommandsEvent> ringBuffer = disruptor.getRingBuffer();
            producer = new DisruptorCommandsEventProducer(ringBuffer);
        } catch (Exception e) {
            throw new CamelliaRedisException(e);
        }
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
