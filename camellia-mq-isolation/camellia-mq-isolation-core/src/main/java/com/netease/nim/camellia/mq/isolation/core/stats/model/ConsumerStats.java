package com.netease.nim.camellia.mq.isolation.core.stats.model;

/**
 * Created by caojiajun on 2024/2/7
 */
public class ConsumerStats {
    private LatencyStats latencyStats = new LatencyStats();
    private SpendStats spendStats = new SpendStats();

    public LatencyStats getLatencyStats() {
        return latencyStats;
    }

    public void setLatencyStats(LatencyStats latencyStats) {
        this.latencyStats = latencyStats;
    }

    public SpendStats getSpendStats() {
        return spendStats;
    }

    public void setSpendStats(SpendStats spendStats) {
        this.spendStats = spendStats;
    }
}
