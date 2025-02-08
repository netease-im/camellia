package com.netease.nim.camellia.redis.proxy.monitor.model;

import com.netease.nim.camellia.redis.proxy.upstream.local.storage.compress.CompressType;


/**
 * Created by caojiajun on 2025/2/8
 */
public class LocalStorageKeyBucketStats {

    private CompressType compressType;
    private long count;
    private double keyCountAvg;
    private long keyCountMax;
    private double compressSizeAvg;
    private long compressSizeMax;
    private double decompressSizeAvg;
    private long decompressSizeMax;

    public CompressType getCompressType() {
        return compressType;
    }

    public void setCompressType(CompressType compressType) {
        this.compressType = compressType;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public double getKeyCountAvg() {
        return keyCountAvg;
    }

    public void setKeyCountAvg(double keyCountAvg) {
        this.keyCountAvg = keyCountAvg;
    }

    public long getKeyCountMax() {
        return keyCountMax;
    }

    public void setKeyCountMax(long keyCountMax) {
        this.keyCountMax = keyCountMax;
    }

    public double getCompressSizeAvg() {
        return compressSizeAvg;
    }

    public void setCompressSizeAvg(double compressSizeAvg) {
        this.compressSizeAvg = compressSizeAvg;
    }

    public long getCompressSizeMax() {
        return compressSizeMax;
    }

    public void setCompressSizeMax(long compressSizeMax) {
        this.compressSizeMax = compressSizeMax;
    }

    public double getDecompressSizeAvg() {
        return decompressSizeAvg;
    }

    public void setDecompressSizeAvg(double decompressSizeAvg) {
        this.decompressSizeAvg = decompressSizeAvg;
    }

    public long getDecompressSizeMax() {
        return decompressSizeMax;
    }

    public void setDecompressSizeMax(long decompressSizeMax) {
        this.decompressSizeMax = decompressSizeMax;
    }
}
