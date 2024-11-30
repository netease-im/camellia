package com.netease.nim.camellia.mq.isolation.core.config;

import com.netease.nim.camellia.mq.isolation.core.mq.ConsumerBuilder;

/**
 * Created by caojiajun on 2024/2/7
 */
public class ConsumerManagerConfig {

    private DispatcherConfig dispatcherConfig;

    private ConsumerMqInfoConfig mqInfoConfig;

    private ConsumerBuilder consumerBuilder;

    public DispatcherConfig getDispatcherConfig() {
        return dispatcherConfig;
    }

    public void setDispatcherConfig(DispatcherConfig dispatcherConfig) {
        this.dispatcherConfig = dispatcherConfig;
    }

    public ConsumerMqInfoConfig getMqInfoConfig() {
        return mqInfoConfig;
    }

    public void setMqInfoConfig(ConsumerMqInfoConfig mqInfoConfig) {
        this.mqInfoConfig = mqInfoConfig;
    }

    public ConsumerBuilder getConsumerBuilder() {
        return consumerBuilder;
    }

    public void setConsumerBuilder(ConsumerBuilder consumerBuilder) {
        this.consumerBuilder = consumerBuilder;
    }
}
