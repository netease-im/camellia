package com.netease.nim.camellia.mq.isolation.domain;

/**
 * Created by caojiajun on 2024/2/6
 */
public class MqIsolationMsgPacket {
    private String msgId;
    private MqIsolationMsg msg;
    private long msgCreateTime;//消息创建时间，一旦创建，不会发生变化
    private long msgPushMqTime;//消息最近一次投递到mq时间，如果重试或者自动隔离等导致消息重新进入mq，则时间会被重置
    private int retry;//重试次数，默认0

    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    public MqIsolationMsg getMsg() {
        return msg;
    }

    public void setMsg(MqIsolationMsg msg) {
        this.msg = msg;
    }

    public long getMsgCreateTime() {
        return msgCreateTime;
    }

    public void setMsgCreateTime(long msgCreateTime) {
        this.msgCreateTime = msgCreateTime;
    }

    public long getMsgPushMqTime() {
        return msgPushMqTime;
    }

    public void setMsgPushMqTime(long msgPushMqTime) {
        this.msgPushMqTime = msgPushMqTime;
    }

    public int getRetry() {
        return retry;
    }

    public void setRetry(int retry) {
        this.retry = retry;
    }
}
