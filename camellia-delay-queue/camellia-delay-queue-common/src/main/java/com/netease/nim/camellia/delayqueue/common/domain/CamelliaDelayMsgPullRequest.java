package com.netease.nim.camellia.delayqueue.common.domain;

/**
 * Created by caojiajun on 2022/7/12
 */
public class CamelliaDelayMsgPullRequest {
    private String topic;
    private long ackTimeoutMillis;//ack的超时时间，如果小于等于0，则取服务器默认配置
    private int batch;//批量大小，如果小于等于0，则默认1

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public long getAckTimeoutMillis() {
        return ackTimeoutMillis;
    }

    public void setAckTimeoutMillis(long ackTimeoutMillis) {
        this.ackTimeoutMillis = ackTimeoutMillis;
    }

    public int getBatch() {
        return batch;
    }

    public void setBatch(int batch) {
        this.batch = batch;
    }
}
