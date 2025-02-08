package com.netease.nim.camellia.redis.proxy.monitor.model;

/**
 * Created by caojiajun on 2024/5/21
 */
public class LocalStorageCacheStats {
    private String operation;
    private long memTable;
    private long rowCache;
    private long blockCache;
    private long disk;
    private double memTableHit;
    private double rowCacheHit;
    private double blockCacheHit;
    private double diskHit;

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public long getMemTable() {
        return memTable;
    }

    public void setMemTable(long memTable) {
        this.memTable = memTable;
    }

    public long getRowCache() {
        return rowCache;
    }

    public void setRowCache(long rowCache) {
        this.rowCache = rowCache;
    }

    public long getBlockCache() {
        return blockCache;
    }

    public void setBlockCache(long blockCache) {
        this.blockCache = blockCache;
    }

    public long getDisk() {
        return disk;
    }

    public void setDisk(long disk) {
        this.disk = disk;
    }

    public double getMemTableHit() {
        return memTableHit;
    }

    public void setMemTableHit(double memTableHit) {
        this.memTableHit = memTableHit;
    }

    public double getRowCacheHit() {
        return rowCacheHit;
    }

    public void setRowCacheHit(double rowCacheHit) {
        this.rowCacheHit = rowCacheHit;
    }

    public double getBlockCacheHit() {
        return blockCacheHit;
    }

    public void setBlockCacheHit(double blockCacheHit) {
        this.blockCacheHit = blockCacheHit;
    }

    public double getDiskHit() {
        return diskHit;
    }

    public void setDiskHit(double diskHit) {
        this.diskHit = diskHit;
    }
}
