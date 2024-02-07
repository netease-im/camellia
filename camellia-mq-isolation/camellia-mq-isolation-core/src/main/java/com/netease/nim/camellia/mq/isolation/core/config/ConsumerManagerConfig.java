package com.netease.nim.camellia.mq.isolation.core.config;

import com.netease.nim.camellia.mq.isolation.core.mq.MqInfo;
import com.netease.nim.camellia.mq.isolation.core.mq.TopicType;

import java.util.Set;

/**
 * Created by caojiajun on 2024/2/7
 */
public class ConsumerManagerConfig {
    private ConsumerConfig consumerConfig;
    private ConsumerManagerType type;
    private int reloadConsumerIntervalSeconds = 30;
    private Set<TopicType> excludeTopicTypeSet;
    private Set<MqInfo> excludeMqInfoSet;
    private Set<TopicType> specifyTopicTypeSet;
    private Set<MqInfo> specifyMqInfoSet;

    public ConsumerConfig getConsumerConfig() {
        return consumerConfig;
    }

    public void setConsumerConfig(ConsumerConfig consumerConfig) {
        this.consumerConfig = consumerConfig;
    }

    public ConsumerManagerType getType() {
        return type;
    }

    public void setType(ConsumerManagerType type) {
        this.type = type;
    }

    public int getReloadConsumerIntervalSeconds() {
        return reloadConsumerIntervalSeconds;
    }

    public void setReloadConsumerIntervalSeconds(int reloadConsumerIntervalSeconds) {
        this.reloadConsumerIntervalSeconds = reloadConsumerIntervalSeconds;
    }

    public Set<TopicType> getExcludeTopicTypeSet() {
        return excludeTopicTypeSet;
    }

    public void setExcludeTopicTypeSet(Set<TopicType> excludeTopicTypeSet) {
        this.excludeTopicTypeSet = excludeTopicTypeSet;
    }

    public Set<MqInfo> getExcludeMqInfoSet() {
        return excludeMqInfoSet;
    }

    public void setExcludeMqInfoSet(Set<MqInfo> excludeMqInfoSet) {
        this.excludeMqInfoSet = excludeMqInfoSet;
    }

    public Set<TopicType> getSpecifyTopicTypeSet() {
        return specifyTopicTypeSet;
    }

    public void setSpecifyTopicTypeSet(Set<TopicType> specifyTopicTypeSet) {
        this.specifyTopicTypeSet = specifyTopicTypeSet;
    }

    public Set<MqInfo> getSpecifyMqInfoSet() {
        return specifyMqInfoSet;
    }

    public void setSpecifyMqInfoSet(Set<MqInfo> specifyMqInfoSet) {
        this.specifyMqInfoSet = specifyMqInfoSet;
    }
}
