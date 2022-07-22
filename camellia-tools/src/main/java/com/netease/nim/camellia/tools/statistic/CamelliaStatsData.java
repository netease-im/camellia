package com.netease.nim.camellia.tools.statistic;


/**
 * Created by caojiajun on 2022/7/21
 */
public class CamelliaStatsData {
    private final long count;
    private final double avg;
    private final long max;
    private final long sum;

    public CamelliaStatsData(long count, double avg, long max, long sum) {
        this.count = count;
        this.avg = avg;
        this.max = max;
        this.sum = sum;
    }

    public long getCount() {
        return count;
    }

    public double getAvg() {
        return avg;
    }

    public long getMax() {
        return max;
    }

    public long getSum() {
        return sum;
    }
}
