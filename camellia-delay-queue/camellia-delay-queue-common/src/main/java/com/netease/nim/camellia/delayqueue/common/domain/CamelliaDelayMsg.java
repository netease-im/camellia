package com.netease.nim.camellia.delayqueue.common.domain;

/**
 * Created by caojiajun on 2022/7/7
 */
public class CamelliaDelayMsg {
    //归属的topic
    private String topic;
    //消息id，topic内唯一
    private String msgId;
    //消息内容
    private String msg;
    //消息产生的时间
    private long produceTime;
    //消息需要触发的消费时间
    private long triggerTime;
    //消息过期时间，超过本时间还没有消费掉，则直接丢弃
    private long expireTime;
    //消息消费的最大重试次数，超过最大重试次数，则直接丢弃
    private int maxRetry;
    //当前到第几次重试，初始值是0，第一次被拉取后设置为1
    private int retry;
    //消息的状态，参考CamelliaDelayMsgStatus
    private int status;

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

    public long getProduceTime() {
        return produceTime;
    }

    public void setProduceTime(long produceTime) {
        this.produceTime = produceTime;
    }

    public long getTriggerTime() {
        return triggerTime;
    }

    public void setTriggerTime(long triggerTime) {
        this.triggerTime = triggerTime;
    }

    public long getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(long expireTime) {
        this.expireTime = expireTime;
    }

    public int getMaxRetry() {
        return maxRetry;
    }

    public void setMaxRetry(int maxRetry) {
        this.maxRetry = maxRetry;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getRetry() {
        return retry;
    }

    public void setRetry(int retry) {
        this.retry = retry;
    }
}
