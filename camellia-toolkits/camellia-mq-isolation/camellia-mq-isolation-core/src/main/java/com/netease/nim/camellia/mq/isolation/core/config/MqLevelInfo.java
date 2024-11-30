package com.netease.nim.camellia.mq.isolation.core.config;

import com.netease.nim.camellia.mq.isolation.core.mq.MqInfo;

import java.util.List;

/**
 * Created by caojiajun on 2024/3/26
 */
public class MqLevelInfo {
    private List<MqInfo> mqInfoList;
    private TimeRange timeRange;

    public List<MqInfo> getMqInfoList() {
        return mqInfoList;
    }

    public void setMqInfoList(List<MqInfo> mqInfoList) {
        this.mqInfoList = mqInfoList;
    }

    public TimeRange getTimeRange() {
        return timeRange;
    }

    public void setTimeRange(TimeRange timeRange) {
        this.timeRange = timeRange;
    }
}
