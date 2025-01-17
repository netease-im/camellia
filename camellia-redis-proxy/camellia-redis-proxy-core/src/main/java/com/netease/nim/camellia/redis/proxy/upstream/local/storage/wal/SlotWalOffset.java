package com.netease.nim.camellia.redis.proxy.upstream.local.storage.wal;

/**
 * Created by caojiajun on 2025/1/14
 */
public record SlotWalOffset(long recordId, long fileId, long fileOffset) {
}
