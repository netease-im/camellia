package com.netease.nim.camellia.external.call.common;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by caojiajun on 2023/2/28
 */
public class ExternalCallConsumeStats {

    private String instanceId;
    private String namespace;
    private long timestamp;
    private List<Stats> statsList = new ArrayList<>();

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public List<Stats> getStatsList() {
        return statsList;
    }

    public void setStatsList(List<Stats> statsList) {
        this.statsList = statsList;
    }

    public static class Stats {
        private String isolationKey;
        private long count;
        private long success;
        private long fail;
        private double spendAvg;
        private double spendMax;
        private long p50;
        private long p75;
        private long p90;
        private long p95;
        private long p99;
        private long p999;

        public String getIsolationKey() {
            return isolationKey;
        }

        public void setIsolationKey(String isolationKey) {
            this.isolationKey = isolationKey;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }

        public long getSuccess() {
            return success;
        }

        public void setSuccess(long success) {
            this.success = success;
        }

        public long getFail() {
            return fail;
        }

        public void setFail(long fail) {
            this.fail = fail;
        }

        public double getSpendAvg() {
            return spendAvg;
        }

        public void setSpendAvg(double spendAvg) {
            this.spendAvg = spendAvg;
        }

        public double getSpendMax() {
            return spendMax;
        }

        public void setSpendMax(double spendMax) {
            this.spendMax = spendMax;
        }

        public long getP50() {
            return p50;
        }

        public void setP50(long p50) {
            this.p50 = p50;
        }

        public long getP75() {
            return p75;
        }

        public void setP75(long p75) {
            this.p75 = p75;
        }

        public long getP90() {
            return p90;
        }

        public void setP90(long p90) {
            this.p90 = p90;
        }

        public long getP95() {
            return p95;
        }

        public void setP95(long p95) {
            this.p95 = p95;
        }

        public long getP99() {
            return p99;
        }

        public void setP99(long p99) {
            this.p99 = p99;
        }

        public long getP999() {
            return p999;
        }

        public void setP999(long p999) {
            this.p999 = p999;
        }
    }
}
