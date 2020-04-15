package com.netease.nim.camellia.redis.proxy.hbase.monitor;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * Created by caojiajun on 2020/3/5.
 */
public class RedisHBaseStats {

    private List<MethodStats> methodStatsList = new ArrayList<>();
    private List<MethodCacheHitStats> methodCacheHitStatsList = new ArrayList<>();
    private ZSetStats zSetStats = new ZSetStats();

    public List<MethodStats> getMethodStatsList() {
        return methodStatsList;
    }

    public void setMethodStatsList(List<MethodStats> methodStatsList) {
        this.methodStatsList = methodStatsList;
    }

    public List<MethodCacheHitStats> getMethodCacheHitStatsList() {
        return methodCacheHitStatsList;
    }

    public void setMethodCacheHitStatsList(List<MethodCacheHitStats> methodCacheHitStatsList) {
        this.methodCacheHitStatsList = methodCacheHitStatsList;
    }

    public ZSetStats getzSetStats() {
        return zSetStats;
    }

    public void setzSetStats(ZSetStats zSetStats) {
        this.zSetStats = zSetStats;
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

    public static class MethodStats {
        private String method;
        private OpeType opeType;
        private long count;

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public OpeType getOpeType() {
            return opeType;
        }

        public void setOpeType(OpeType opeType) {
            this.opeType = opeType;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }
    }

    public static class MethodCacheHitStats {
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
}
