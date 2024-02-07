package com.netease.nim.camellia.mq.isolation.stats.model;

/**
 * Created by caojiajun on 2024/2/6
 */
public class ConsumerBizStats {

    private String namespace;
    private String bizId;
    private long success;
    private long fail;
    private double spendAvg;
    private double spendMax;
    private double p50;
    private double p90;
    private double p99;

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getBizId() {
        return bizId;
    }

    public void setBizId(String bizId) {
        this.bizId = bizId;
    }

    public long getSuccess() {
        return success;
    }

    public void setSuccess(long success) {
        this.success = success;
    }

    public long getFail() {
        return fail;
    }

    public void setFail(long fail) {
        this.fail = fail;
    }

    public double getSpendAvg() {
        return spendAvg;
    }

    public void setSpendAvg(double spendAvg) {
        this.spendAvg = spendAvg;
    }

    public double getSpendMax() {
        return spendMax;
    }

    public void setSpendMax(double spendMax) {
        this.spendMax = spendMax;
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
