package com.netease.nim.camellia.mq.isolation.core.stats.model;

import com.netease.nim.camellia.mq.isolation.core.mq.MqInfo;

/**
 * Created by caojiajun on 2024/2/7
 */
public class MqInfoLatency {
    private MqInfo mqInfo;
    private long count;
    private double avg;
    private double max;
    private double p50;
    private double p90;
    private double p99;

    public MqInfo getMqInfo() {
        return mqInfo;
    }

    public void setMqInfo(MqInfo mqInfo) {
        this.mqInfo = mqInfo;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public double getAvg() {
        return avg;
    }

    public void setAvg(double avg) {
        this.avg = avg;
    }

    public double getMax() {
        return max;
    }

    public void setMax(double max) {
        this.max = max;
    }

    public double getP50() {
        return p50;
    }

    public void setP50(double p50) {
        this.p50 = p50;
    }

    public double getP90() {
        return p90;
    }

    public void setP90(double p90) {
        this.p90 = p90;
    }

    public double getP99() {
        return p99;
    }

    public void setP99(double p99) {
        this.p99 = p99;
    }
}
