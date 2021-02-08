package com.netease.nim.camellia.redis.proxy.hbase.monitor;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * Created by caojiajun on 2020/12/22
 */
public class RedisHBaseStats {

    private List<Stats> statsList = new ArrayList<>();
    private List<Stats2> stats2List = new ArrayList<>();
    private List<QueueStats> queueStatsList = new ArrayList<>();

    public List<Stats> getStatsList() {
        return statsList;
    }

    public void setStatsList(List<Stats> statsList) {
        this.statsList = statsList;
    }

    public List<Stats2> getStats2List() {
        return stats2List;
    }

    public void setStats2List(List<Stats2> stats2List) {
        this.stats2List = stats2List;
    }

    public List<QueueStats> getQueueStatsList() {
        return queueStatsList;
    }

    public void setQueueStatsList(List<QueueStats> queueStatsList) {
        this.queueStatsList = queueStatsList;
    }

    public static class Stats {
        private String method;
        private String desc;
        private long count;

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public String getDesc() {
            return desc;
        }

        public void setDesc(String desc) {
            this.desc = desc;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }
    }

    public static class Stats2 {
        private String method;
        private long count;
        private double cacheHitPercent;

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public double getCacheHitPercent() {
            return cacheHitPercent;
        }

        public void setCacheHitPercent(double cacheHitPercent) {
            this.cacheHitPercent = cacheHitPercent;
        }
    }

    public static class QueueStats {
        private String queueName;
        private int queueSize;

        public String getQueueName() {
            return queueName;
        }

        public void setQueueName(String queueName) {
            this.queueName = queueName;
        }

        public int getQueueSize() {
            return queueSize;
        }

        public void setQueueSize(int queueSize) {
            this.queueSize = queueSize;
        }
    }
}
