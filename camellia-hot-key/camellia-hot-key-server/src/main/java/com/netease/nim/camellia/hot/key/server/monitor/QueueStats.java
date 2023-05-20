package com.netease.nim.camellia.hot.key.server.monitor;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by caojiajun on 2023/5/11
 */
public class QueueStats {
    private int queueNum;
    private List<Stats> statsList = new ArrayList<>();

    public int getQueueNum() {
        return queueNum;
    }

    public void setQueueNum(int queueNum) {
        this.queueNum = queueNum;
    }

    public List<Stats> getStatsList() {
        return statsList;
    }

    public void setStatsList(List<Stats> statsList) {
        this.statsList = statsList;
    }

    public static class Stats {
        private long id;
        private long pendingSize;
        private long discardCount;

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public long getPendingSize() {
            return pendingSize;
        }

        public void setPendingSize(long pendingSize) {
            this.pendingSize = pendingSize;
        }

        public long getDiscardCount() {
            return discardCount;
        }

        public void setDiscardCount(long discardCount) {
            this.discardCount = discardCount;
        }
    }
}
