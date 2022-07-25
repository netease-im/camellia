package com.netease.nim.camellia.delayqueue.common.domain;

/**
 * Created by caojiajun on 2022/7/12
 */
public class CamelliaDelayMsgSendRequest {

    //归属的topic
    private String topic;
    //消息id，如果不填，则由服务器生成，如果客户端生成，则需要自己保证唯一性，如果相同的msgId，则服务器会覆盖
    private String msgId;
    //消息内容
    private String msg;
    //延迟时长，单位ms
    private long delayMillis;
    //延迟达到后的消费过期时间，单位ms，如果小于等于0，则取服务器默认配置
    private long ttlMillis;
    //延迟达到后，消费的最大重试次数，如果小于等于0，则取服务器默认配置
    private int maxRetry;

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

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public long getDelayMillis() {
        return delayMillis;
    }

    public void setDelayMillis(long delayMillis) {
        this.delayMillis = delayMillis;
    }

    public long getTtlMillis() {
        return ttlMillis;
    }

    public void setTtlMillis(long ttlMillis) {
        this.ttlMillis = ttlMillis;
    }

    public int getMaxRetry() {
        return maxRetry;
    }

    public void setMaxRetry(int maxRetry) {
        this.maxRetry = maxRetry;
    }
}
