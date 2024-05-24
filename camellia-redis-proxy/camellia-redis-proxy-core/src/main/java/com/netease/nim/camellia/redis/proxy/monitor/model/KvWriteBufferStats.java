package com.netease.nim.camellia.redis.proxy.monitor.model;

/**
 * Created by caojiajun on 2024/5/23
 */
public class KvWriteBufferStats {
    private String namespace;
    private String type;
    private long writeBufferCacheHit;
    private long syncWrite;
    private long asyncWrite;
    private long asyncWriteDone;
    private long pending;

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getWriteBufferCacheHit() {
        return writeBufferCacheHit;
    }

    public void setWriteBufferCacheHit(long writeBufferCacheHit) {
        this.writeBufferCacheHit = writeBufferCacheHit;
    }

    public long getSyncWrite() {
        return syncWrite;
    }

    public void setSyncWrite(long syncWrite) {
        this.syncWrite = syncWrite;
    }

    public long getAsyncWrite() {
        return asyncWrite;
    }

    public void setAsyncWrite(long asyncWrite) {
        this.asyncWrite = asyncWrite;
    }

    public long getAsyncWriteDone() {
        return asyncWriteDone;
    }

    public void setAsyncWriteDone(long asyncWriteDone) {
        this.asyncWriteDone = asyncWriteDone;
    }

    public long getPending() {
        return pending;
    }

    public void setPending(long pending) {
        this.pending = pending;
    }
}
