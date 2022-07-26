package com.netease.nim.camellia.delayqueue.server;


/**
 * Created by caojiajun on 2022/7/22
 */
public class CamelliaDelayQueueTopicInfo {

    private String topic;
    private long waitingQueueSize;
    private WaitingQueueInfo waitingQueueInfo;
    private long readyQueueSize;
    private long ackQueueSize;

    public static class WaitingQueueInfo {
        private long sizeOf0To1min;
        private long sizeOf1minTo10min;
        private long sizeOf10minTo30min;
        private long sizeOf30minTo1hour;
        private long sizeOf1hourTo6hour;
        private long sizeOf6hourTo1day;
        private long sizeOf1dayTo7day;
        private long sizeOf7dayTo30day;
        private long sizeOf30dayToInfinite;

        public long getSizeOf0To1min() {
            return sizeOf0To1min;
        }

        public void setSizeOf0To1min(long sizeOf0To1min) {
            this.sizeOf0To1min = sizeOf0To1min;
        }

        public long getSizeOf1minTo10min() {
            return sizeOf1minTo10min;
        }

        public void setSizeOf1minTo10min(long sizeOf1minTo10min) {
            this.sizeOf1minTo10min = sizeOf1minTo10min;
        }

        public long getSizeOf10minTo30min() {
            return sizeOf10minTo30min;
        }

        public void setSizeOf10minTo30min(long sizeOf10minTo30min) {
            this.sizeOf10minTo30min = sizeOf10minTo30min;
        }

        public long getSizeOf30minTo1hour() {
            return sizeOf30minTo1hour;
        }

        public void setSizeOf30minTo1hour(long sizeOf30minTo1hour) {
            this.sizeOf30minTo1hour = sizeOf30minTo1hour;
        }

        public long getSizeOf1hourTo6hour() {
            return sizeOf1hourTo6hour;
        }

        public void setSizeOf1hourTo6hour(long sizeOf1hourTo6hour) {
            this.sizeOf1hourTo6hour = sizeOf1hourTo6hour;
        }

        public long getSizeOf6hourTo1day() {
            return sizeOf6hourTo1day;
        }

        public void setSizeOf6hourTo1day(long sizeOf6hourTo1day) {
            this.sizeOf6hourTo1day = sizeOf6hourTo1day;
        }

        public long getSizeOf1dayTo7day() {
            return sizeOf1dayTo7day;
        }

        public void setSizeOf1dayTo7day(long sizeOf1dayTo7day) {
            this.sizeOf1dayTo7day = sizeOf1dayTo7day;
        }

        public long getSizeOf7dayTo30day() {
            return sizeOf7dayTo30day;
        }

        public void setSizeOf7dayTo30day(long sizeOf7dayTo30day) {
            this.sizeOf7dayTo30day = sizeOf7dayTo30day;
        }

        public long getSizeOf30dayToInfinite() {
            return sizeOf30dayToInfinite;
        }

        public void setSizeOf30dayToInfinite(long sizeOf30dayToInfinite) {
            this.sizeOf30dayToInfinite = sizeOf30dayToInfinite;
        }
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public long getWaitingQueueSize() {
        return waitingQueueSize;
    }

    public void setWaitingQueueSize(long waitingQueueSize) {
        this.waitingQueueSize = waitingQueueSize;
    }

    public WaitingQueueInfo getWaitingQueueInfo() {
        return waitingQueueInfo;
    }

    public void setWaitingQueueInfo(WaitingQueueInfo waitingQueueInfo) {
        this.waitingQueueInfo = waitingQueueInfo;
    }

    public long getReadyQueueSize() {
        return readyQueueSize;
    }

    public void setReadyQueueSize(long readyQueueSize) {
        this.readyQueueSize = readyQueueSize;
    }

    public long getAckQueueSize() {
        return ackQueueSize;
    }

    public void setAckQueueSize(long ackQueueSize) {
        this.ackQueueSize = ackQueueSize;
    }
}
