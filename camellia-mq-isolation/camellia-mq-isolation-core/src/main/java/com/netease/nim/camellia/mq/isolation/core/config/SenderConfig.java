package com.netease.nim.camellia.mq.isolation.core.config;

import com.netease.nim.camellia.mq.isolation.core.MqIsolationController;
import com.netease.nim.camellia.mq.isolation.core.mq.MqSender;

/**
 * Created by caojiajun on 2024/2/6
 */
public class SenderConfig {
    private MqSender mqSender;
    private MqIsolationController controller;
    private int cacheCapacity = 10000;
    private int cacheExpireSeconds = 10;

    public MqSender getMqSender() {
        return mqSender;
    }

    public void setMqSender(MqSender mqSender) {
        this.mqSender = mqSender;
    }

    public MqIsolationController getController() {
        return controller;
    }

    public void setController(MqIsolationController controller) {
        this.controller = controller;
    }

    public int getCacheCapacity() {
        return cacheCapacity;
    }

    public void setCacheCapacity(int cacheCapacity) {
        this.cacheCapacity = cacheCapacity;
    }

    public int getCacheExpireSeconds() {
        return cacheExpireSeconds;
    }

    public void setCacheExpireSeconds(int cacheExpireSeconds) {
        this.cacheExpireSeconds = cacheExpireSeconds;
    }
}
