package com.netease.nim.camellia.hot.key.server.monitor;

import com.netease.nim.camellia.hot.key.server.callback.HotKeyInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by caojiajun on 2023/5/11
 */
public class HotKeyServerStats {

    private String applicationName;
    private int monitorIntervalSeconds;
    private long connectCount;
    private QueueStats queueStats;
    private TrafficStats trafficStats;
    private List<HotKeyInfo> hotKeyInfoList = new ArrayList<>();

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public int getMonitorIntervalSeconds() {
        return monitorIntervalSeconds;
    }

    public void setMonitorIntervalSeconds(int monitorIntervalSeconds) {
        this.monitorIntervalSeconds = monitorIntervalSeconds;
    }

    public long getConnectCount() {
        return connectCount;
    }

    public void setConnectCount(long connectCount) {
        this.connectCount = connectCount;
    }

    public QueueStats getQueueStats() {
        return queueStats;
    }

    public void setQueueStats(QueueStats queueStats) {
        this.queueStats = queueStats;
    }

    public TrafficStats getTrafficStats() {
        return trafficStats;
    }

    public void setTrafficStats(TrafficStats trafficStats) {
        this.trafficStats = trafficStats;
    }

    public List<HotKeyInfo> getHotKeyInfoList() {
        return hotKeyInfoList;
    }

    public void setHotKeyInfoList(List<HotKeyInfo> hotKeyInfoList) {
        this.hotKeyInfoList = hotKeyInfoList;
    }
}
