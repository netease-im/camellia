package com.netease.nim.camellia.redis.proxy.hbase.monitor;

import java.util.*;

/**
 *
 * Created by caojiajun on 2020/3/5.
 */
public class RedisHBaseStats {

    private List<ReadMethodStats> readMethodStatsList = new ArrayList<>();
    private List<ReadMethodCacheHitStats> readMethodCacheHitStatsList = new ArrayList<>();
    private List<WriteMethodStats> writeMethodStatsList = new ArrayList<>();
    private ZSetStats zSetStats = new ZSetStats();
    private List<TopicStats> topicStatsList = new ArrayList<>();

    public List<ReadMethodStats> getReadMethodStatsList() {
        return readMethodStatsList;
    }

    public void setReadMethodStatsList(List<ReadMethodStats> readMethodStatsList) {
        this.readMethodStatsList = readMethodStatsList;
    }

    public List<ReadMethodCacheHitStats> getReadMethodCacheHitStatsList() {
        return readMethodCacheHitStatsList;
    }

    public void setReadMethodCacheHitStatsList(List<ReadMethodCacheHitStats> readMethodCacheHitStatsList) {
        this.readMethodCacheHitStatsList = readMethodCacheHitStatsList;
    }

    public List<WriteMethodStats> getWriteMethodStatsList() {
        return writeMethodStatsList;
    }

    public void setWriteMethodStatsList(List<WriteMethodStats> writeMethodStatsList) {
        this.writeMethodStatsList = writeMethodStatsList;
    }

    public ZSetStats getzSetStats() {
        return zSetStats;
    }

    public void setzSetStats(ZSetStats zSetStats) {
        this.zSetStats = zSetStats;
    }

    public List<TopicStats> getTopicStatsList() {
        return topicStatsList;
    }

    public void setTopicStatsList(List<TopicStats> topicStatsList) {
        this.topicStatsList = topicStatsList;
    }

    public static class ZSetStats {
        private double zsetValueSizeAvg;
        private long zsetValueSizeMax;
        private long zsetValueHitThresholdCount;
        private long zsetValueNotHitThresholdCount;

        public double getZsetValueSizeAvg() {
            return zsetValueSizeAvg;
        }

        public void setZsetValueSizeAvg(double zsetValueSizeAvg) {
            this.zsetValueSizeAvg = zsetValueSizeAvg;
        }

        public long getZsetValueSizeMax() {
            return zsetValueSizeMax;
        }

        public void setZsetValueSizeMax(long zsetValueSizeMax) {
            this.zsetValueSizeMax = zsetValueSizeMax;
        }

        public long getZsetValueHitThresholdCount() {
            return zsetValueHitThresholdCount;
        }

        public void setZsetValueHitThresholdCount(long zsetValueHitThresholdCount) {
            this.zsetValueHitThresholdCount = zsetValueHitThresholdCount;
        }

        public long getZsetValueNotHitThresholdCount() {
            return zsetValueNotHitThresholdCount;
        }

        public void setZsetValueNotHitThresholdCount(long zsetValueNotHitThresholdCount) {
            this.zsetValueNotHitThresholdCount = zsetValueNotHitThresholdCount;
        }
    }

    public static class WriteMethodStats {
        private String method;
        private WriteOpeType opeType;
        private long count;

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public WriteOpeType getOpeType() {
            return opeType;
        }

        public void setOpeType(WriteOpeType opeType) {
            this.opeType = opeType;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }
    }

    public static class ReadMethodStats {
        private String method;
        private ReadOpeType opeType;
        private long count;

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public ReadOpeType getOpeType() {
            return opeType;
        }

        public void setOpeType(ReadOpeType opeType) {
            this.opeType = opeType;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }
    }

    public static class ReadMethodCacheHitStats {
        private String method;
        private long count;
        private double cacheHitPercent;

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }

        public double getCacheHitPercent() {
            return cacheHitPercent;
        }

        public void setCacheHitPercent(double cacheHitPercent) {
            this.cacheHitPercent = cacheHitPercent;
        }
    }

    public static class TopicStats {
        private String topic;
        private long count;
        private long length;

        public String getTopic() {
            return topic;
        }

        public void setTopic(String topic) {
            this.topic = topic;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }

        public long getLength() {
            return length;
        }

        public void setLength(long length) {
            this.length = length;
        }
    }
}
