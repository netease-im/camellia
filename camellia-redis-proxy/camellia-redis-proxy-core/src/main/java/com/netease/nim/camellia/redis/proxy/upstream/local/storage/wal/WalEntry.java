package com.netease.nim.camellia.redis.proxy.upstream.local.storage.wal;

import com.netease.nim.camellia.redis.proxy.upstream.local.storage.command.LocalStorageReadWrite;

/**
 * Created by caojiajun on 2025/1/16
 */
public interface WalEntry {

    void recover(short slot, LocalStorageReadWrite readWrite) throws Exception;
}
