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

    private int intervalSeconds;
    private long clientConnectCount;
    private long count;
    private long totalReadCount;
    private long totalWriteCount;
    private List<TotalStats> totalStatsList = new ArrayList<>();
    private List<BidBgroupStats> bidBgroupStatsList = new ArrayList<>();
    private List<DetailStats> detailStatsList = new ArrayList<>();
    private Map<String, Long> failMap = new HashMap<>();
    private List<SpendStats> spendStatsList = new ArrayList<>();
    private List<BidBgroupSpendStats> bidBgroupSpendStatsList = new ArrayList<>();
    private List<ResourceStats> resourceStatsList = new ArrayList<>();
    private List<ResourceCommandStats> resourceCommandStatsList = new ArrayList<>();
    private List<ResourceBidBgroupStats> resourceBidBgroupStatsList = new ArrayList<>();
    private List<ResourceBidBgroupCommandStats> resourceBidBgroupCommandStatsList = new ArrayList<>();
    private List<RouteConf> routeConfList = new ArrayList<>();

    public int getIntervalSeconds() {
        return intervalSeconds;
    }

    public void setIntervalSeconds(int intervalSeconds) {
        this.intervalSeconds = intervalSeconds;
    }

    public long getClientConnectCount() {
        return clientConnectCount;
    }

    public void setClientConnectCount(long clientConnectCount) {
        this.clientConnectCount = clientConnectCount;
    }

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

    public List<BidBgroupSpendStats> getBidBgroupSpendStatsList() {
        return bidBgroupSpendStatsList;
    }

    public void setBidBgroupSpendStatsList(List<BidBgroupSpendStats> bidBgroupSpendStatsList) {
        this.bidBgroupSpendStatsList = bidBgroupSpendStatsList;
    }

    public List<ResourceCommandStats> getResourceCommandStatsList() {
        return resourceCommandStatsList;
    }

    public void setResourceCommandStatsList(List<ResourceCommandStats> resourceCommandStatsList) {
        this.resourceCommandStatsList = resourceCommandStatsList;
    }

    public List<ResourceBidBgroupCommandStats> getResourceBidBgroupCommandStatsList() {
        return resourceBidBgroupCommandStatsList;
    }

    public void setResourceBidBgroupCommandStatsList(List<ResourceBidBgroupCommandStats> resourceBidBgroupCommandStatsList) {
        this.resourceBidBgroupCommandStatsList = resourceBidBgroupCommandStatsList;
    }

    public List<ResourceStats> getResourceStatsList() {
        return resourceStatsList;
    }

    public void setResourceStatsList(List<ResourceStats> resourceStatsList) {
        this.resourceStatsList = resourceStatsList;
    }

    public List<ResourceBidBgroupStats> getResourceBidBgroupStatsList() {
        return resourceBidBgroupStatsList;
    }

    public void setResourceBidBgroupStatsList(List<ResourceBidBgroupStats> resourceBidBgroupStatsList) {
        this.resourceBidBgroupStatsList = resourceBidBgroupStatsList;
    }

    public List<RouteConf> getRouteConfList() {
        return routeConfList;
    }

    public void setRouteConfList(List<RouteConf> routeConfList) {
        this.routeConfList = routeConfList;
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

    public static class BidBgroupSpendStats {
        private Long bid;
        private String bgroup;
        private String command;
        private long count;
        private double avgSpendMs;
        private double maxSpendMs;

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

    public static class ResourceStats {
        private String resource;
        private long count;

        public String getResource() {
            return resource;
        }

        public void setResource(String resource) {
            this.resource = resource;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }
    }

    public static class ResourceCommandStats {
        private String resource;
        private String command;
        private long count;

        public String getResource() {
            return resource;
        }

        public void setResource(String resource) {
            this.resource = resource;
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

    public static class ResourceBidBgroupStats {
        private Long bid;
        private String bgroup;
        private String resource;
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

        public String getResource() {
            return resource;
        }

        public void setResource(String resource) {
            this.resource = resource;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }
    }

    public static class ResourceBidBgroupCommandStats {
        private Long bid;
        private String bgroup;
        private String resource;
        private String command;
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

        public String getResource() {
            return resource;
        }

        public void setResource(String resource) {
            this.resource = resource;
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

    public static class RouteConf {
        private Long bid;
        private String bgroup;
        private String resourceTable;
        private long updateTime;

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

        public String getResourceTable() {
            return resourceTable;
        }

        public void setResourceTable(String resourceTable) {
            this.resourceTable = resourceTable;
        }

        public long getUpdateTime() {
            return updateTime;
        }

        public void setUpdateTime(long updateTime) {
            this.updateTime = updateTime;
        }
    }
}
