package com.netease.nim.camellia.mq.isolation.config;

import com.netease.nim.camellia.mq.isolation.MqIsolationController;
import com.netease.nim.camellia.mq.isolation.mq.MqSender;

/**
 * Created by caojiajun on 2024/2/6
 */
public class SenderConfig {
    private MqSender mqSender;
    private MqIsolationController controller;
    private int reportIntervalSeconds = 10;
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

    public int getReportIntervalSeconds() {
        return reportIntervalSeconds;
    }

    public void setReportIntervalSeconds(int reportIntervalSeconds) {
        this.reportIntervalSeconds = reportIntervalSeconds;
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
