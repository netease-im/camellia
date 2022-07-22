package com.netease.nim.camellia.delayqueue.server;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by caojiajun on 2022/7/22
 */
public class CamelliaDelayQueueMonitorData {

    private List<RequestStats> requestStatsList = new ArrayList<>();
    private List<TimeGapStats> pullMsgTimeGapStatsList = new ArrayList<>();
    private List<TimeGapStats> readyQueueTimeGapStatsList = new ArrayList<>();

    public List<RequestStats> getRequestStatsList() {
        return requestStatsList;
    }

    public void setRequestStatsList(List<RequestStats> requestStatsList) {
        this.requestStatsList = requestStatsList;
    }

    public List<TimeGapStats> getPullMsgTimeGapStatsList() {
        return pullMsgTimeGapStatsList;
    }

    public void setPullMsgTimeGapStatsList(List<TimeGapStats> pullMsgTimeGapStatsList) {
        this.pullMsgTimeGapStatsList = pullMsgTimeGapStatsList;
    }

    public List<TimeGapStats> getReadyQueueTimeGapStatsList() {
        return readyQueueTimeGapStatsList;
    }

    public void setReadyQueueTimeGapStatsList(List<TimeGapStats> readyQueueTimeGapStatsList) {
        this.readyQueueTimeGapStatsList = readyQueueTimeGapStatsList;
    }

    public static class TimeGapStats {
        private final String topic;
        private final long count;
        private final double avg;
        private final long max;

        public TimeGapStats(String topic, long count, double avg, long max) {
            this.topic = topic;
            this.count = count;
            this.avg = avg;
            this.max = max;
        }

        public String getTopic() {
            return topic;
        }

        public long getCount() {
            return count;
        }

        public double getAvg() {
            return avg;
        }

        public long getMax() {
            return max;
        }
    }

    public static class RequestStats {
        private final String topic;
        private final long sendMsg;
        private final long pullMsg;
        private final long deleteMsg;
        private final long ackMsg;
        private final long getMsg;
        private final long triggerMsgReady;
        private final long triggerMsgEndLife;
        private final long triggerMsgTimeout;

        public RequestStats(String topic, long sendMsg, long pullMsg, long deleteMsg, long ackMsg,
                            long getMsg, long triggerMsgReady, long triggerMsgEndLife, long triggerMsgTimeout) {
            this.topic = topic;
            this.sendMsg = sendMsg;
            this.pullMsg = pullMsg;
            this.deleteMsg = deleteMsg;
            this.ackMsg = ackMsg;
            this.getMsg = getMsg;
            this.triggerMsgReady = triggerMsgReady;
            this.triggerMsgEndLife = triggerMsgEndLife;
            this.triggerMsgTimeout = triggerMsgTimeout;
        }

        public String getTopic() {
            return topic;
        }

        public long getSendMsg() {
            return sendMsg;
        }

        public long getPullMsg() {
            return pullMsg;
        }

        public long getDeleteMsg() {
            return deleteMsg;
        }

        public long getAckMsg() {
            return ackMsg;
        }

        public long getGetMsg() {
            return getMsg;
        }

        public long getTriggerMsgReady() {
            return triggerMsgReady;
        }

        public long getTriggerMsgEndLife() {
            return triggerMsgEndLife;
        }

        public long getTriggerMsgTimeout() {
            return triggerMsgTimeout;
        }
    }
}
