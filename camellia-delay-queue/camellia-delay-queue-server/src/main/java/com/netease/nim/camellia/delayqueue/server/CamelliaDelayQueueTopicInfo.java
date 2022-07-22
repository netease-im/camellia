package com.netease.nim.camellia.delayqueue.server;


/**
 * Created by caojiajun on 2022/7/22
 */
public class CamelliaDelayQueueTopicInfo {

    private final String topic;
    private final long waitingQueueSize;
    private final long readyQueueSize;
    private final long ackQueueSize;

    public CamelliaDelayQueueTopicInfo(String topic, long waitingQueueSize, long readyQueueSize, long ackQueueSize) {
        this.topic = topic;
        this.waitingQueueSize = waitingQueueSize;
        this.readyQueueSize = readyQueueSize;
        this.ackQueueSize = ackQueueSize;
    }

    public String getTopic() {
        return topic;
    }

    public long getWaitingQueueSize() {
        return waitingQueueSize;
    }

    public long getReadyQueueSize() {
        return readyQueueSize;
    }

    public long getAckQueueSize() {
        return ackQueueSize;
    }
}
