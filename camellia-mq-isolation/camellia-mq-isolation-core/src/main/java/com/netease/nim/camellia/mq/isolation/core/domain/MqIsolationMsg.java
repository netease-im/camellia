package com.netease.nim.camellia.mq.isolation.core.domain;

/**
 * Created by caojiajun on 2024/2/4
 */
public class MqIsolationMsg {

    private String namespace;
    private String bizId;
    private String msg;

    public MqIsolationMsg(String namespace, String bizId, String msg) {
        this.namespace = namespace;
        this.bizId = bizId;
        this.msg = msg;
    }

    public MqIsolationMsg() {
    }

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

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
