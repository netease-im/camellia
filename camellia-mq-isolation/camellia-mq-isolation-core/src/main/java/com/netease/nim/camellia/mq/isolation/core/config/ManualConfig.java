package com.netease.nim.camellia.mq.isolation.core.config;

import com.netease.nim.camellia.mq.isolation.core.mq.MqInfo;

/**
 * Created by caojiajun on 2024/2/19
 */
public class ManualConfig {
    private MatchType matchType;
    private String bizId;
    private MqInfo mqInfo;
    private boolean autoIsolation;

    public MatchType getMatchType() {
        return matchType;
    }

    public void setMatchType(MatchType matchType) {
        this.matchType = matchType;
    }

    public String getBizId() {
        return bizId;
    }

    public void setBizId(String bizId) {
        this.bizId = bizId;
    }

    public MqInfo getMqInfo() {
        return mqInfo;
    }

    public void setMqInfo(MqInfo mqInfo) {
        this.mqInfo = mqInfo;
    }

    public boolean isAutoIsolation() {
        return autoIsolation;
    }

    public void setAutoIsolation(boolean autoIsolation) {
        this.autoIsolation = autoIsolation;
    }
}
