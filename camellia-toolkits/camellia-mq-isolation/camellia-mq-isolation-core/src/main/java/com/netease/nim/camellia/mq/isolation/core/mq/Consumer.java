package com.netease.nim.camellia.mq.isolation.core.mq;

import com.netease.nim.camellia.mq.isolation.core.CamelliaMqIsolationMsgDispatcher;

import java.util.concurrent.CompletableFuture;

/**
 * Created by caojiajun on 2024/2/19
 */
public abstract class Consumer {

    private final MqInfo mqInfo;
    private final CamelliaMqIsolationMsgDispatcher dispatcher;

    public Consumer(MqInfo mqInfo, CamelliaMqIsolationMsgDispatcher dispatcher) {
        this.mqInfo = mqInfo;
        this.dispatcher = dispatcher;
    }

    /**
     * start
     */
    public abstract void start();

    /**
     * stop
     */
    public abstract void stop();

    /**
     * 在业务实现的start0方法中，在获取到消息后，需要调用本方法去执行实际的消费逻辑
     * @param data 数据
     * @return 消费结果
     */
    protected final CompletableFuture<Boolean> onMsg(byte[] data) {
        return dispatcher.onMsg(mqInfo, data);
    }
}
