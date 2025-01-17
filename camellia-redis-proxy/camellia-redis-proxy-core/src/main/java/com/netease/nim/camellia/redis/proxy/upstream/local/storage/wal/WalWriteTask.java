package com.netease.nim.camellia.redis.proxy.upstream.local.storage.wal;

import java.util.concurrent.CompletableFuture;

/**
 * Created by caojiajun on 2025/1/17
 */
public record WalWriteTask(LogRecord record, long fileId, CompletableFuture<WalWriteResult> future) {
}
