package com.netease.nim.camellia.mq.isolation.kafka;

import com.netease.nim.camellia.mq.isolation.core.CamelliaMqIsolationMsgDispatcher;
import com.netease.nim.camellia.mq.isolation.core.mq.Consumer;
import com.netease.nim.camellia.mq.isolation.core.mq.ConsumerBuilder;
import com.netease.nim.camellia.mq.isolation.core.mq.MqInfo;

import java.util.Properties;

/**
 * Created by caojiajun on 2024/2/19
 */
public class KafkaConsumerBuilder implements ConsumerBuilder {

    private final Properties properties;

    public KafkaConsumerBuilder(Properties properties) {
        this.properties = properties;
    }

    @Override
    public Consumer newConsumer(MqInfo mqInfo, CamelliaMqIsolationMsgDispatcher dispatcher) {
        return new KafkaConsumer(mqInfo, dispatcher, properties);
    }
}
