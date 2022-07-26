package com.netease.nim.camellia.delayqueue.server;

import com.netease.nim.camellia.delayqueue.common.conf.CamelliaDelayQueueConstants;

/**
 * Created by caojiajun on 2022/7/14
 */
public class CamelliaDelayQueueServerConfig {

    private String namespace = "default";

    private int checkTriggerThreadNum = CamelliaDelayQueueConstants.checkTriggerThreadNum;
    private int checkTimeoutThreadNum = CamelliaDelayQueueConstants.checkTimeoutThreadNum;

    private long msgScheduleMillis = CamelliaDelayQueueConstants.msgScheduleMillis;
    private int scheduleThreadNum = CamelliaDelayQueueConstants.scheduleThreadNum;
    private long topicScheduleSeconds = CamelliaDelayQueueConstants.topicScheduleSeconds;

    private long ttlMillis = CamelliaDelayQueueConstants.ttlMillis;
    private int maxRetry = CamelliaDelayQueueConstants.maxRetry;
    private long endLifeMsgExpireMillis = CamelliaDelayQueueConstants.endLifeMsgExpireMillis;
    private long ackTimeoutMillis = CamelliaDelayQueueConstants.ackTimeoutMillis;
    private long topicActiveTagTimeoutMillis = CamelliaDelayQueueConstants.topicActiveTagTimeoutMillis;

    private int monitorIntervalSeconds = CamelliaDelayQueueConstants.monitorIntervalSeconds;

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public int getCheckTriggerThreadNum() {
        return checkTriggerThreadNum;
    }

    public void setCheckTriggerThreadNum(int checkTriggerThreadNum) {
        this.checkTriggerThreadNum = checkTriggerThreadNum;
    }

    public int getCheckTimeoutThreadNum() {
        return checkTimeoutThreadNum;
    }

    public void setCheckTimeoutThreadNum(int checkTimeoutThreadNum) {
        this.checkTimeoutThreadNum = checkTimeoutThreadNum;
    }

    public long getMsgScheduleMillis() {
        return msgScheduleMillis;
    }

    public void setMsgScheduleMillis(long msgScheduleMillis) {
        this.msgScheduleMillis = msgScheduleMillis;
    }

    public int getScheduleThreadNum() {
        return scheduleThreadNum;
    }

    public void setScheduleThreadNum(int scheduleThreadNum) {
        this.scheduleThreadNum = scheduleThreadNum;
    }

    public long getTopicScheduleSeconds() {
        return topicScheduleSeconds;
    }

    public void setTopicScheduleSeconds(long topicScheduleSeconds) {
        this.topicScheduleSeconds = topicScheduleSeconds;
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

    public long getEndLifeMsgExpireMillis() {
        return endLifeMsgExpireMillis;
    }

    public void setEndLifeMsgExpireMillis(long endLifeMsgExpireMillis) {
        this.endLifeMsgExpireMillis = endLifeMsgExpireMillis;
    }

    public long getAckTimeoutMillis() {
        return ackTimeoutMillis;
    }

    public void setAckTimeoutMillis(long ackTimeoutMillis) {
        this.ackTimeoutMillis = ackTimeoutMillis;
    }

    public long getTopicActiveTagTimeoutMillis() {
        return topicActiveTagTimeoutMillis;
    }

    public void setTopicActiveTagTimeoutMillis(long topicActiveTagTimeoutMillis) {
        this.topicActiveTagTimeoutMillis = topicActiveTagTimeoutMillis;
    }

    public int getMonitorIntervalSeconds() {
        return monitorIntervalSeconds;
    }

    public void setMonitorIntervalSeconds(int monitorIntervalSeconds) {
        this.monitorIntervalSeconds = monitorIntervalSeconds;
    }
}
