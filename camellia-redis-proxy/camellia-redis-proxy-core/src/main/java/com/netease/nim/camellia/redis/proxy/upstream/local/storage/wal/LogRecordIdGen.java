package com.netease.nim.camellia.redis.proxy.upstream.local.storage.wal;

/**
 * Created by caojiajun on 2025/1/14
 */
public class LogRecordIdGen {

    public LogRecordIdGen() {
    }

    public long nextId() {
        return System.currentTimeMillis();
    }
}
