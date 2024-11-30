package com.netease.nim.camellia.tools.sys;

/**
 * Created by caojiajun on 2023/12/21
 */
public class CpuUsage {

    private double ratio;//100表示使用掉了1core
    private long cpuNum;

    public double getRatio() {
        return ratio;
    }

    public void setRatio(double ratio) {
        this.ratio = ratio;
    }

    public long getCpuNum() {
        return cpuNum;
    }

    public void setCpuNum(long cpuNum) {
        this.cpuNum = cpuNum;
    }
}
