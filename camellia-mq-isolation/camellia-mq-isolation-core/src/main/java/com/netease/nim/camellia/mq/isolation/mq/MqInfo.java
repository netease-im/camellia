package com.netease.nim.camellia.mq.isolation.mq;

import java.util.Objects;

/**
 * Created by caojiajun on 2024/2/4
 */
public class MqInfo {
    private String mq;
    private String topic;

    public MqInfo(String mq, String topic) {
        this.mq = mq;
        this.topic = topic;
    }

    public MqInfo() {
    }

    public String getMq() {
        return mq;
    }

    public void setMq(String mq) {
        this.mq = mq;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MqInfo mqInfo = (MqInfo) o;
        return Objects.equals(mq, mqInfo.mq) && Objects.equals(topic, mqInfo.topic);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mq, topic);
    }

    @Override
    public String toString() {
        return "MqInfo{" +
                "mq='" + mq + '\'' +
                ", topic='" + topic + '\'' +
                '}';
    }
}
