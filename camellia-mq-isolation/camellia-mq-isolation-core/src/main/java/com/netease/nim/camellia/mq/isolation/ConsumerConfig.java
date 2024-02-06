package com.netease.nim.camellia.mq.isolation;

import com.netease.nim.camellia.core.client.env.ThreadContextSwitchStrategy;
import com.netease.nim.camellia.mq.isolation.executor.MsgHandler;
import com.netease.nim.camellia.mq.isolation.mq.MqInfoConfig;
import com.netease.nim.camellia.mq.isolation.mq.MqSender;

/**
 * Created by caojiajun on 2024/2/6
 */
public class ConsumerConfig {

    private int threads;
    private MqInfoConfig mqInfoConfig;
    private MqSender mqSender;
    private MsgHandler msgHandler;
    private ThreadContextSwitchStrategy strategy = new ThreadContextSwitchStrategy.Default();

    public int getThreads() {
        return threads;
    }

    public void setThreads(int threads) {
        this.threads = threads;
    }

    public MqInfoConfig getMqInfoConfig() {
        return mqInfoConfig;
    }

    public void setMqInfoConfig(MqInfoConfig mqInfoConfig) {
        this.mqInfoConfig = mqInfoConfig;
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
