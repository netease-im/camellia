package com.netease.nim.camellia.redis.proxy.upstream.local.storage.command;

import com.netease.nim.camellia.redis.proxy.upstream.local.storage.compact.CompactExecutor;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.wal.Wal;

/**
 * Created by caojiajun on 2025/1/10
 */
public class CommandConfig {

    private CompactExecutor compactExecutor;
    private LocalStorageReadWrite readWrite;
    private Wal wal;

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

    public Wal getWal() {
        return wal;
    }

    public void setWal(Wal wal) {
        this.wal = wal;
    }
}
