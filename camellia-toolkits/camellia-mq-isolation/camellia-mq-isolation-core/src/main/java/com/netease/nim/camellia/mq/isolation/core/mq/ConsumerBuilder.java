package com.netease.nim.camellia.mq.isolation.core.mq;

import com.netease.nim.camellia.mq.isolation.core.CamelliaMqIsolationMsgDispatcher;

/**
 * Created by caojiajun on 2024/2/19
 */
public interface ConsumerBuilder {

    Consumer newConsumer(MqInfo mqInfo, CamelliaMqIsolationMsgDispatcher dispatcher);

}
