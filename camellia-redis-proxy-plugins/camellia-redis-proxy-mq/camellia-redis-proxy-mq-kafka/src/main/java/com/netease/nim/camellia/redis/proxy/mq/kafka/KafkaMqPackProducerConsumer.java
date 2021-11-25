package com.netease.nim.camellia.redis.proxy.mq.kafka;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.command.async.interceptor.CommandInterceptResponse;
import com.netease.nim.camellia.redis.proxy.command.async.interceptor.CommandInterceptor;
import com.netease.nim.camellia.redis.proxy.mq.common.MqMultiWriteCommandInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KafkaMqPackProducerConsumer implements CommandInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(KafkaMqPackProducerConsumer.class);
    private final MqMultiWriteCommandInterceptor producer;

    public KafkaMqPackProducerConsumer() {
        //init producer
        producer = new MqMultiWriteCommandInterceptor(new KafkaMqPackSender());
        logger.info("start kafka producer, producer = {}", producer.getClass().getName());
        //init consumer
        KafkaMqPackConsumer consumer = new KafkaMqPackConsumer();
        logger.info("start kafka consumer, consumer = {}", consumer.getClass().getName());
    }

    @Override
    public CommandInterceptResponse check(Command command) {
        return producer.check(command);
    }
}
