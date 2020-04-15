package com.netease.nim.camellia.core.api;


import java.util.ArrayList;
import java.util.List;

/**
 *
 * Created by caojiajun on 2019/6/5.
 */
public class ResourceStats {

    public static final String OPE_WRITE = "W";
    public static final String OPE_READ = "R";

    private String source;
    private Long bid;
    private String bgroup;
    private List<Stats> statsList = new ArrayList<>();
    private List<StatsDetail> statsDetailList = new ArrayList<>();

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public List<Stats> getStatsList() {
        return statsList;
    }

    public void setStatsList(List<Stats> statsList) {
        this.statsList = statsList;
    }

    public List<StatsDetail> getStatsDetailList() {
        return statsDetailList;
    }

    public void setStatsDetailList(List<StatsDetail> statsDetailList) {
        this.statsDetailList = statsDetailList;
    }

    public Long getBid() {
        return bid;
    }

    public void setBid(Long bid) {
        this.bid = bid;
    }

    public String getBgroup() {
        return bgroup;
    }

    public void setBgroup(String bgroup) {
        this.bgroup = bgroup;
    }

    public static class Stats {
        private String resource;
        private String ope;
        private long count;

        public Stats() {
        }

        public Stats(String resource, String ope) {
            this.resource = resource;
            this.ope = ope;
        }

        public String getResource() {
            return resource;
        }

        public void setResource(String resource) {
            this.resource = resource;
        }

        public String getOpe() {
            return ope;
        }

        public void setOpe(String ope) {
            this.ope = ope;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }
    }

    public static class StatsDetail {
        private String resource;
        private String className;
        private String methodName;
        private String ope;
        private long count;

        public StatsDetail() {
        }

        public StatsDetail(String resource, String className, String methodName, String ope) {
            this.resource = resource;
            this.className = className;
            this.methodName = methodName;
            this.ope = ope;
        }

        public String getResource() {
            return resource;
        }

        public void setResource(String resource) {
            this.resource = resource;
        }

        public String getClassName() {
            return className;
        }

        public void setClassName(String className) {
            this.className = className;
        }

        public String getMethodName() {
            return methodName;
        }

        public void setMethodName(String methodName) {
            this.methodName = methodName;
        }

        public String getOpe() {
            return ope;
        }

        public void setOpe(String ope) {
            this.ope = ope;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }
    }
}
