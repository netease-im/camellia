package com.netease.nim.camellia.mq.isolation.core.stats.model;

import com.netease.nim.camellia.mq.isolation.core.mq.MqInfo;
import com.netease.nim.camellia.mq.isolation.core.mq.TopicType;

/**
 * Created by caojiajun on 2024/2/23
 */
public class NamespaceBizIdDistributionStats {
    private String namespace;
    private String bizId;
    private TopicType topicType;
    private MqInfo mqInfo;
    private long count;

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getBizId() {
        return bizId;
    }

    public void setBizId(String bizId) {
        this.bizId = bizId;
    }

    public TopicType getTopicType() {
        return topicType;
    }

    public void setTopicType(TopicType topicType) {
        this.topicType = topicType;
    }

    public MqInfo getMqInfo() {
        return mqInfo;
    }

    public void setMqInfo(MqInfo mqInfo) {
        this.mqInfo = mqInfo;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}
