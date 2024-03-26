package com.netease.nim.camellia.mq.isolation.core.config;

import com.netease.nim.camellia.mq.isolation.core.mq.MqInfo;

import java.util.List;
import java.util.Map;

/**
 * Created by caojiajun on 2024/3/6
 */
public class ReadableMqIsolationConfig {

    private String namespace;
    private Map<String, String> mqAlias;
    private Integer senderStatsIntervalSeconds;
    private Integer senderStatsExpireSeconds;
    private Integer consumerStatsIntervalSeconds;
    private Integer consumerStatsExpireSeconds;
    private Integer senderHeavyTrafficThreshold1;
    private Integer senderHeavyTrafficThreshold2;
    private Double senderHeavyTrafficPercent;
    private Double consumerFailRateThreshold;
    private List<MqLevelInfo> levelInfoList;
    private List<MqLevelInfo> errorLevelInfoList;
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

    public Map<String, String> getMqAlias() {
        return mqAlias;
    }

    public void setMqAlias(Map<String, String> mqAlias) {
        this.mqAlias = mqAlias;
    }

    public Integer getSenderStatsIntervalSeconds() {
        return senderStatsIntervalSeconds;
    }

    public void setSenderStatsIntervalSeconds(Integer senderStatsIntervalSeconds) {
        this.senderStatsIntervalSeconds = senderStatsIntervalSeconds;
    }

    public Integer getSenderStatsExpireSeconds() {
        return senderStatsExpireSeconds;
    }

    public void setSenderStatsExpireSeconds(Integer senderStatsExpireSeconds) {
        this.senderStatsExpireSeconds = senderStatsExpireSeconds;
    }

    public Integer getConsumerStatsIntervalSeconds() {
        return consumerStatsIntervalSeconds;
    }

    public void setConsumerStatsIntervalSeconds(Integer consumerStatsIntervalSeconds) {
        this.consumerStatsIntervalSeconds = consumerStatsIntervalSeconds;
    }

    public Integer getConsumerStatsExpireSeconds() {
        return consumerStatsExpireSeconds;
    }

    public void setConsumerStatsExpireSeconds(Integer consumerStatsExpireSeconds) {
        this.consumerStatsExpireSeconds = consumerStatsExpireSeconds;
    }

    public Integer getSenderHeavyTrafficThreshold1() {
        return senderHeavyTrafficThreshold1;
    }

    public void setSenderHeavyTrafficThreshold1(Integer senderHeavyTrafficThreshold1) {
        this.senderHeavyTrafficThreshold1 = senderHeavyTrafficThreshold1;
    }

    public Integer getSenderHeavyTrafficThreshold2() {
        return senderHeavyTrafficThreshold2;
    }

    public void setSenderHeavyTrafficThreshold2(Integer senderHeavyTrafficThreshold2) {
        this.senderHeavyTrafficThreshold2 = senderHeavyTrafficThreshold2;
    }

    public Double getSenderHeavyTrafficPercent() {
        return senderHeavyTrafficPercent;
    }

    public void setSenderHeavyTrafficPercent(Double senderHeavyTrafficPercent) {
        this.senderHeavyTrafficPercent = senderHeavyTrafficPercent;
    }

    public Double getConsumerFailRateThreshold() {
        return consumerFailRateThreshold;
    }

    public void setConsumerFailRateThreshold(Double consumerFailRateThreshold) {
        this.consumerFailRateThreshold = consumerFailRateThreshold;
    }

    public List<MqLevelInfo> getLevelInfoList() {
        return levelInfoList;
    }

    public void setLevelInfoList(List<MqLevelInfo> levelInfoList) {
        this.levelInfoList = levelInfoList;
    }

    public List<MqLevelInfo> getErrorLevelInfoList() {
        return errorLevelInfoList;
    }

    public void setErrorLevelInfoList(List<MqLevelInfo> errorLevelInfoList) {
        this.errorLevelInfoList = errorLevelInfoList;
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
}
