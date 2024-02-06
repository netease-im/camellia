package com.netease.nim.camellia.mq.isolation.config;

import com.netease.nim.camellia.core.client.env.ThreadContextSwitchStrategy;
import com.netease.nim.camellia.mq.isolation.executor.MsgHandler;
import com.netease.nim.camellia.mq.isolation.mq.MqSender;

/**
 * Created by caojiajun on 2024/2/6
 */
public class ConsumerConfig {

    private int threads;
    private MqIsolationConfig mqIsolationConfig;
    private MqSender mqSender;
    private MsgHandler msgHandler;
    private ThreadContextSwitchStrategy strategy = new ThreadContextSwitchStrategy.Default();

    public int getThreads() {
        return threads;
    }

    public void setThreads(int threads) {
        this.threads = threads;
    }

    public MqIsolationConfig getMqIsolationConfig() {
        return mqIsolationConfig;
    }

    public void setMqIsolationConfig(MqIsolationConfig mqIsolationConfig) {
        this.mqIsolationConfig = mqIsolationConfig;
    }

    public MqSender getMqSender() {
        return mqSender;
    }

    public void setMqSender(MqSender mqSender) {
        this.mqSender = mqSender;
    }

    public MsgHandler getMsgHandler() {
        return msgHandler;
    }

    public void setMsgHandler(MsgHandler msgHandler) {
        this.msgHandler = msgHandler;
    }

    public ThreadContextSwitchStrategy getStrategy() {
        return strategy;
    }

    public void setStrategy(ThreadContextSwitchStrategy strategy) {
        this.strategy = strategy;
    }
}
