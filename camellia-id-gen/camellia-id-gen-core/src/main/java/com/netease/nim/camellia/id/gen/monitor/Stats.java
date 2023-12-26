package com.netease.nim.camellia.id.gen.monitor;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by caojiajun on 2023/12/26
 */
public class Stats {

    private List<UriStats> statsList = new ArrayList<>();

    public List<UriStats> getStatsList() {
        return statsList;
    }

    public void setStatsList(List<UriStats> statsList) {
        this.statsList = statsList;
    }

    public static class UriStats {
        private String uri;
        private int code;
        private long count;
        private long spendAvg;
        private long spendMax;
        private long spendP50;
        private long spendP90;
        private long spendP99;

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }

        public long getSpendAvg() {
            return spendAvg;
        }

        public void setSpendAvg(long spendAvg) {
            this.spendAvg = spendAvg;
        }

        public long getSpendMax() {
            return spendMax;
        }

        public void setSpendMax(long spendMax) {
            this.spendMax = spendMax;
        }

        public long getSpendP50() {
            return spendP50;
        }

        public void setSpendP50(long spendP50) {
            this.spendP50 = spendP50;
        }

        public long getSpendP90() {
            return spendP90;
        }

        public void setSpendP90(long spendP90) {
            this.spendP90 = spendP90;
        }

        public long getSpendP99() {
            return spendP99;
        }

        public void setSpendP99(long spendP99) {
            this.spendP99 = spendP99;
        }
    }
}
