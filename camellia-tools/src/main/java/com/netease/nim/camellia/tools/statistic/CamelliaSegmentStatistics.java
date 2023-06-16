package com.netease.nim.camellia.tools.statistic;

import java.util.*;

/**
 * Created by caojiajun on 2023/6/16
 */
public class CamelliaSegmentStatistics {

    private final long startTime;
    private final List<SegmentItem> list = new ArrayList<>();

    private static class SegmentItem {
        String segment;
        long time;

        public SegmentItem(String segment, long time) {
            this.segment = segment;
            this.time = time;
        }
    }

    public CamelliaSegmentStatistics() {
        startTime = System.currentTimeMillis();
    }

    public void update(String segment) {
        list.add(new SegmentItem(segment, System.currentTimeMillis()));
    }

    public CamelliaSegmentStatisticsData end(String lastSegment) {
        update(lastSegment);
        long totalSpend = 0;
        Map<String, Long> segmentSpendMap = new HashMap<>();
        for (int i=0; i<list.size(); i++) {
            SegmentItem item = list.get(i);
            long spend;
            if (i == 0) {
                spend = item.time - startTime;
            } else {
                spend = item.time - list.get(i - 1).time;
            }
            segmentSpendMap.put(item.segment, spend);
            if (i == list.size() - 1) {
                totalSpend = item.time - startTime;
            }
        }
        return new CamelliaSegmentStatisticsData(totalSpend, segmentSpendMap);
    }

}
