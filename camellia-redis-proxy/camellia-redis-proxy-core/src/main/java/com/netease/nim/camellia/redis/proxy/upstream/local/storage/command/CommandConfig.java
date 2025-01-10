package com.netease.nim.camellia.redis.proxy.upstream.local.storage.command;

import com.netease.nim.camellia.redis.proxy.upstream.local.storage.compact.CompactExecutor;

/**
 * Created by caojiajun on 2025/1/10
 */
public class CommandConfig {

    private CompactExecutor compactExecutor;
    private LocalStorageReadWrite readWrite;

    public CompactExecutor getCompactExecutor() {
        return compactExecutor;
    }

    public void setCompactExecutor(CompactExecutor compactExecutor) {
        this.compactExecutor = compactExecutor;
    }

    public LocalStorageReadWrite getReadWrite() {
        return readWrite;
    }

    public void setReadWrite(LocalStorageReadWrite readWrite) {
        this.readWrite = readWrite;
    }
}
