package com.netease.nim.camellia.redis.proxy.monitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * Created by caojiajun on 2019/11/28.
 */
public class Stats {

    private long count;
    private long totalReadCount;
    private long totalWriteCount;
    private List<TotalStats> totalStatsList = new ArrayList<>();
    private List<BidBgroupStats> bidBgroupStatsList = new ArrayList<>();
    private List<DetailStats> detailStatsList = new ArrayList<>();
    private Map<String, Long> failMap = new HashMap<>();
    private List<SpendStats> spendStatsList = new ArrayList<>();

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public long getTotalReadCount() {
        return totalReadCount;
    }

    public void setTotalReadCount(long totalReadCount) {
        this.totalReadCount = totalReadCount;
    }

    public long getTotalWriteCount() {
        return totalWriteCount;
    }

    public void setTotalWriteCount(long totalWriteCount) {
        this.totalWriteCount = totalWriteCount;
    }

    public List<TotalStats> getTotalStatsList() {
        return totalStatsList;
    }

    public void setTotalStatsList(List<TotalStats> totalStatsList) {
        this.totalStatsList = totalStatsList;
    }

    public List<BidBgroupStats> getBidBgroupStatsList() {
        return bidBgroupStatsList;
    }

    public void setBidBgroupStatsList(List<BidBgroupStats> bidBgroupStatsList) {
        this.bidBgroupStatsList = bidBgroupStatsList;
    }

    public List<DetailStats> getDetailStatsList() {
        return detailStatsList;
    }

    public void setDetailStatsList(List<DetailStats> detailStatsList) {
        this.detailStatsList = detailStatsList;
    }

    public Map<String, Long> getFailMap() {
        return failMap;
    }

    public void setFailMap(Map<String, Long> failMap) {
        this.failMap = failMap;
    }

    public List<SpendStats> getSpendStatsList() {
        return spendStatsList;
    }

    public void setSpendStatsList(List<SpendStats> spendStatsList) {
        this.spendStatsList = spendStatsList;
    }

    public static class BidBgroupStats {
        private Long bid;
        private String bgroup;
        private long count;

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

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }
    }

    public static class TotalStats {
        private String command;
        private long count;

        public TotalStats(String command) {
            this.command = command;
        }

        public String getCommand() {
            return command;
        }

        public void setCommand(String command) {
            this.command = command;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }
    }

    public static class DetailStats {
        private Long bid;
        private String bgroup;
        private String command;
        private long count;

        public DetailStats(Long bid, String bgroup, String command, long count) {
            this.bid = bid;
            this.bgroup = bgroup;
            this.command = command;
            this.count = count;
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

        public String getCommand() {
            return command;
        }

        public void setCommand(String command) {
            this.command = command;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }
    }

    public static class SpendStats {
        private String command;
        private long count;
        private double avgSpendMs;
        private double maxSpendMs;

        public String getCommand() {
            return command;
        }

        public void setCommand(String command) {
            this.command = command;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }

        public double getAvgSpendMs() {
            return avgSpendMs;
        }

        public void setAvgSpendMs(double avgSpendMs) {
            this.avgSpendMs = avgSpendMs;
        }

        public double getMaxSpendMs() {
            return maxSpendMs;
        }

        public void setMaxSpendMs(double maxSpendMs) {
            this.maxSpendMs = maxSpendMs;
        }
    }
}
