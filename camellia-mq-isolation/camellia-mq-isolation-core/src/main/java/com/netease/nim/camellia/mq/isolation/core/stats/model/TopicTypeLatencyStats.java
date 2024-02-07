package com.netease.nim.camellia.mq.isolation.core.stats.model;

import com.netease.nim.camellia.mq.isolation.core.mq.TopicType;

/**
 * Created by caojiajun on 2024/2/7
 */
public class TopicTypeLatencyStats {

    private String namespace;
    private TopicType topicType;
    private long count;
    private double avg;
    private double max;
    private double p50;
    private double p90;
    private double p99;

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public TopicType getTopicType() {
        return topicType;
    }

    public void setTopicType(TopicType topicType) {
        this.topicType = topicType;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public double getAvg() {
        return avg;
    }

    public void setAvg(double avg) {
        this.avg = avg;
    }

    public double getMax() {
        return max;
    }

    public void setMax(double max) {
        this.max = max;
    }

    public double getP50() {
        return p50;
    }

    public void setP50(double p50) {
        this.p50 = p50;
    }

    public double getP90() {
        return p90;
    }

    public void setP90(double p90) {
        this.p90 = p90;
    }

    public double getP99() {
        return p99;
    }

    public void setP99(double p99) {
        this.p99 = p99;
    }
}
