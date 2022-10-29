package com.netease.nim.camellia.delayqueue.server.springboot;

import com.netease.nim.camellia.core.util.SysUtils;
import com.netease.nim.camellia.delayqueue.common.conf.CamelliaDelayQueueConstants;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Created by caojiajun on 2022/7/20
 */
@ConfigurationProperties(prefix = "camellia-delay-queue-server")
public class CamelliaDelayQueueServerProperties {

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

    private int longPollingScheduledThreadSize = CamelliaDelayQueueConstants.longPollingScheduledThreadSize;
    private int longPollingScheduledQueueSize = CamelliaDelayQueueConstants.longPollingScheduledQueueSize;
    private int longPollingMsgReadyCallbackThreadSize = CamelliaDelayQueueConstants.longPollingMsgReadyCallbackThreadSize;
    private int longPollingMsgReadyCallbackQueueSize = CamelliaDelayQueueConstants.longPollingMsgReadyCallbackQueueSize;
    private int longPollingTaskQueueSize = CamelliaDelayQueueConstants.longPollingTaskQueueSize;
    private long longPollingTimeoutMillis = 10000;

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

    public int getLongPollingScheduledThreadSize() {
        return longPollingScheduledThreadSize;
    }

    public void setLongPollingScheduledThreadSize(int longPollingScheduledThreadSize) {
        this.longPollingScheduledThreadSize = longPollingScheduledThreadSize;
    }

    public int getLongPollingMsgReadyCallbackThreadSize() {
        return longPollingMsgReadyCallbackThreadSize;
    }

    public void setLongPollingMsgReadyCallbackThreadSize(int longPollingMsgReadyCallbackThreadSize) {
        this.longPollingMsgReadyCallbackThreadSize = longPollingMsgReadyCallbackThreadSize;
    }

    public int getLongPollingMsgReadyCallbackQueueSize() {
        return longPollingMsgReadyCallbackQueueSize;
    }

    public void setLongPollingMsgReadyCallbackQueueSize(int longPollingMsgReadyCallbackQueueSize) {
        this.longPollingMsgReadyCallbackQueueSize = longPollingMsgReadyCallbackQueueSize;
    }

    public int getLongPollingTaskQueueSize() {
        return longPollingTaskQueueSize;
    }

    public void setLongPollingTaskQueueSize(int longPollingTaskQueueSize) {
        this.longPollingTaskQueueSize = longPollingTaskQueueSize;
    }

    public long getLongPollingTimeoutMillis() {
        return longPollingTimeoutMillis;
    }

    public void setLongPollingTimeoutMillis(long longPollingTimeoutMillis) {
        this.longPollingTimeoutMillis = longPollingTimeoutMillis;
    }

    public int getLongPollingScheduledQueueSize() {
        return longPollingScheduledQueueSize;
    }

    public void setLongPollingScheduledQueueSize(int longPollingScheduledQueueSize) {
        this.longPollingScheduledQueueSize = longPollingScheduledQueueSize;
    }
}
