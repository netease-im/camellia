package com.netease.nim.camellia.tools.statistic;

import java.util.Map;

/**
 * Created by caojiajun on 2023/6/16
 */
public class CamelliaSegmentStatisticsData {

    private final long totalSpend;
    private final Map<String, Long> segmentSpendMap;

    public CamelliaSegmentStatisticsData(long totalSpend, Map<String, Long> segmentSpendMap) {
        this.totalSpend = totalSpend;
        this.segmentSpendMap = segmentSpendMap;
    }

    public final long getTotalSpend() {
        return totalSpend;
    }

    public final Map<String, Long> getSegmentSpendMap() {
        return segmentSpendMap;
    }
}
