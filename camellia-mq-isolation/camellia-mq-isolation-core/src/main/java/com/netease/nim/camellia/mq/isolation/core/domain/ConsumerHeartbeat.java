package com.netease.nim.camellia.mq.isolation.core.domain;

import com.netease.nim.camellia.mq.isolation.core.mq.MqInfo;

/**
 * Created by caojiajun on 2024/2/20
 */
public class ConsumerHeartbeat {
    private String instanceId;
    private String host;
    private String namespace;
    private MqInfo mqInfo;
    private long timestamp;

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public MqInfo getMqInfo() {
        return mqInfo;
    }

    public void setMqInfo(MqInfo mqInfo) {
        this.mqInfo = mqInfo;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
