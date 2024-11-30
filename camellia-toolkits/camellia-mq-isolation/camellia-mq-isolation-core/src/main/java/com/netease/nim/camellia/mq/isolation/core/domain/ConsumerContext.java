package com.netease.nim.camellia.mq.isolation.core.domain;

import com.netease.nim.camellia.mq.isolation.core.mq.MqInfo;
import com.netease.nim.camellia.mq.isolation.core.mq.TopicType;

/**
 * Created by caojiajun on 2024/2/4
 */
public class ConsumerContext {

    private MqInfo mqInfo;
    private TopicType topicType;
    private MqIsolationMsg msg;
    private int retry;
    private long msgCreateTime;
    private long msgPushMqTime;

    public MqInfo getMqInfo() {
        return mqInfo;
    }

    public void setMqInfo(MqInfo mqInfo) {
        this.mqInfo = mqInfo;
    }

    public TopicType getTopicType() {
        return topicType;
    }

    public void setTopicType(TopicType topicType) {
        this.topicType = topicType;
    }

    public MqIsolationMsg getMsg() {
        return msg;
    }

    public void setMsg(MqIsolationMsg msg) {
        this.msg = msg;
    }

    public int getRetry() {
        return retry;
    }

    public void setRetry(int retry) {
        this.retry = retry;
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
}
