package com.netease.nim.camellia.delayqueue.common.domain;

/**
 * Created by caojiajun on 2022/7/12
 */
public class CamelliaDelayMsgDeleteRequest {

    //归属的topic
    private String topic;
    //消息id，topic内唯一
    private String msgId;

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }
}
