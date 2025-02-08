package com.netease.nim.camellia.redis.proxy.monitor.model;

/**
 * Created by caojiajun on 2025/2/8
 */
public class LocalStorageFileStats {

    private String file;
    private long readSize;
    private long writeSize;
    private TimeStats readTime;
    private TimeStats writeTime;

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public long getReadSize() {
        return readSize;
    }

    public void setReadSize(long readSize) {
        this.readSize = readSize;
    }

    public long getWriteSize() {
        return writeSize;
    }

    public void setWriteSize(long writeSize) {
        this.writeSize = writeSize;
    }

    public TimeStats getReadTime() {
        return readTime;
    }

    public void setReadTime(TimeStats readTime) {
        this.readTime = readTime;
    }

    public TimeStats getWriteTime() {
        return writeTime;
    }

    public void setWriteTime(TimeStats writeTime) {
        this.writeTime = writeTime;
    }
}
