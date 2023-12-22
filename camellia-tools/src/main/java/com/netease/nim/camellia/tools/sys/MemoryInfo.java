package com.netease.nim.camellia.tools.sys;

/**
 * Created by caojiajun on 2023/11/29
 */
public class MemoryInfo {
    private long freeMemory;
    private long totalMemory;
    private long maxMemory;
    private long heapMemoryInit;
    private long heapMemoryUsed;
    private long heapMemoryMax;
    private long heapMemoryCommitted;
    private long nonHeapMemoryInit;
    private long nonHeapMemoryUsed;
    private long nonHeapMemoryMax;
    private long nonHeapMemoryCommitted;
    private long nettyDirectMemory;

    public long getFreeMemory() {
        return freeMemory;
    }

    public void setFreeMemory(long freeMemory) {
        this.freeMemory = freeMemory;
    }

    public long getTotalMemory() {
        return totalMemory;
    }

    public void setTotalMemory(long totalMemory) {
        this.totalMemory = totalMemory;
    }

    public long getMaxMemory() {
        return maxMemory;
    }

    public void setMaxMemory(long maxMemory) {
        this.maxMemory = maxMemory;
    }

    public long getHeapMemoryInit() {
        return heapMemoryInit;
    }

    public void setHeapMemoryInit(long heapMemoryInit) {
        this.heapMemoryInit = heapMemoryInit;
    }

    public long getHeapMemoryUsed() {
        return heapMemoryUsed;
    }

    public void setHeapMemoryUsed(long heapMemoryUsed) {
        this.heapMemoryUsed = heapMemoryUsed;
    }

    public long getHeapMemoryMax() {
        return heapMemoryMax;
    }

    public void setHeapMemoryMax(long heapMemoryMax) {
        this.heapMemoryMax = heapMemoryMax;
    }

    public long getHeapMemoryCommitted() {
        return heapMemoryCommitted;
    }

    public void setHeapMemoryCommitted(long heapMemoryCommitted) {
        this.heapMemoryCommitted = heapMemoryCommitted;
    }

    public long getNonHeapMemoryInit() {
        return nonHeapMemoryInit;
    }

    public void setNonHeapMemoryInit(long nonHeapMemoryInit) {
        this.nonHeapMemoryInit = nonHeapMemoryInit;
    }

    public long getNonHeapMemoryUsed() {
        return nonHeapMemoryUsed;
    }

    public void setNonHeapMemoryUsed(long nonHeapMemoryUsed) {
        this.nonHeapMemoryUsed = nonHeapMemoryUsed;
    }

    public long getNonHeapMemoryMax() {
        return nonHeapMemoryMax;
    }

    public void setNonHeapMemoryMax(long nonHeapMemoryMax) {
        this.nonHeapMemoryMax = nonHeapMemoryMax;
    }

    public long getNonHeapMemoryCommitted() {
        return nonHeapMemoryCommitted;
    }

    public void setNonHeapMemoryCommitted(long nonHeapMemoryCommitted) {
        this.nonHeapMemoryCommitted = nonHeapMemoryCommitted;
    }

    public long getNettyDirectMemory() {
        return nettyDirectMemory;
    }

    public void setNettyDirectMemory(long nettyDirectMemory) {
        this.nettyDirectMemory = nettyDirectMemory;
    }
}
