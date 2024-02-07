package com.netease.nim.camellia.mq.isolation.core.config;

import com.netease.nim.camellia.mq.isolation.core.mq.MqInfo;

import java.util.List;

/**
 * Created by caojiajun on 2024/2/6
 */
public class MqIsolationConfig {

    private String namespace;

    private List<MqInfo> fast;

    private List<MqInfo> fastError;

    private List<MqInfo> slow;

    private List<MqInfo> slowError;

    private List<MqInfo> retryLevel0;

    private List<MqInfo> retryLevel1;

    private List<MqInfo> autoIsolationLevel0;

    private List<MqInfo> autoIsolationLevel1;

    private List<ManualConfig> manualConfigs;

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public List<MqInfo> getFast() {
        return fast;
    }

    public void setFast(List<MqInfo> fast) {
        this.fast = fast;
    }

    public List<MqInfo> getFastError() {
        return fastError;
    }

    public void setFastError(List<MqInfo> fastError) {
        this.fastError = fastError;
    }

    public List<MqInfo> getSlow() {
        return slow;
    }

    public void setSlow(List<MqInfo> slow) {
        this.slow = slow;
    }

    public List<MqInfo> getSlowError() {
        return slowError;
    }

    public void setSlowError(List<MqInfo> slowError) {
        this.slowError = slowError;
    }

    public List<MqInfo> getRetryLevel0() {
        return retryLevel0;
    }

    public void setRetryLevel0(List<MqInfo> retryLevel0) {
        this.retryLevel0 = retryLevel0;
    }

    public List<MqInfo> getRetryLevel1() {
        return retryLevel1;
    }

    public void setRetryLevel1(List<MqInfo> retryLevel1) {
        this.retryLevel1 = retryLevel1;
    }

    public List<MqInfo> getAutoIsolationLevel0() {
        return autoIsolationLevel0;
    }

    public void setAutoIsolationLevel0(List<MqInfo> autoIsolationLevel0) {
        this.autoIsolationLevel0 = autoIsolationLevel0;
    }

    public List<MqInfo> getAutoIsolationLevel1() {
        return autoIsolationLevel1;
    }

    public void setAutoIsolationLevel1(List<MqInfo> autoIsolationLevel1) {
        this.autoIsolationLevel1 = autoIsolationLevel1;
    }

    public List<ManualConfig> getManualConfigs() {
        return manualConfigs;
    }

    public void setManualConfigs(List<ManualConfig> manualConfigs) {
        this.manualConfigs = manualConfigs;
    }

    public static class ManualConfig {
        private MatchType matchType;
        private String bizId;
        private MqInfo mqInfo;

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
    }

    public static enum MatchType {
        exact_match,//精准匹配
        prefix_match,//前缀匹配
        ;
    }
}
