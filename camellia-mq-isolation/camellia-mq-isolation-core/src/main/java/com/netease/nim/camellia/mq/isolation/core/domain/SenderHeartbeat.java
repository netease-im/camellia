package com.netease.nim.camellia.mq.isolation.core.domain;

/**
 * Created by caojiajun on 2024/2/20
 */
public class SenderHeartbeat {
    private String instanceId;
    private String host;
    private String namespace;
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

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
