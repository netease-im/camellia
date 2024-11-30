package com.netease.nim.camellia.tools.statistic;


/**
 * Created by caojiajun on 2022/7/21
 */
public class CamelliaStatsData {
    private final long count;
    private final double avg;
    private final long max;
    private final long sum;
    private final long p50;
    private final long p75;
    private final long p90;
    private final long p95;
    private final long p99;
    private final long p999;

    public CamelliaStatsData(long count, double avg, long max, long sum,
                             long p50, long p75, long p90, long p95, long p99, long p999) {
        this.count = count;
        this.avg = avg;
        this.max = max;
        this.sum = sum;
        this.p50 = p50;
        this.p75 = p75;
        this.p90 = p90;
        this.p95 = p95;
        this.p99 = p99;
        this.p999 = p999;
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

    public long getP50() {
        return p50;
    }

    public long getP75() {
        return p75;
    }

    public long getP90() {
        return p90;
    }

    public long getP95() {
        return p95;
    }

    public long getP99() {
        return p99;
    }

    public long getP999() {
        return p999;
    }
}
