package com.netease.nim.camellia.external.call.common;

import java.util.Objects;

/**
 * Created by caojiajun on 2023/2/24
 */
public class MqInfo {
    private String server;
    private String topic;

    public MqInfo() {
    }

    public MqInfo(String server, String topic) {
        this.server = server;
        this.topic = topic;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
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
        return Objects.equals(server, mqInfo.server) && Objects.equals(topic, mqInfo.topic);
    }

    @Override
    public int hashCode() {
        return Objects.hash(server, topic);
    }
}
