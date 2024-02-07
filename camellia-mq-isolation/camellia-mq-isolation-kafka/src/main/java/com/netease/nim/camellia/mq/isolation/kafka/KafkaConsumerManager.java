package com.netease.nim.camellia.mq.isolation.kafka;

import com.netease.nim.camellia.mq.isolation.core.AbstractConsumerManager;
import com.netease.nim.camellia.mq.isolation.core.config.ConsumerManagerConfig;
import com.netease.nim.camellia.mq.isolation.core.mq.MqInfo;

/**
 * Created by caojiajun on 2024/2/7
 */
public class KafkaConsumerManager extends AbstractConsumerManager {

    public KafkaConsumerManager(ConsumerManagerConfig config) {
        super(config);
    }

    @Override
    protected void start0(MqInfo mqInfo) {
        //TODO
    }
}
