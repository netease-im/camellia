package com.netease.nim.camellia.redis.proxy.upstream.local.storage.wal;

import java.util.List;

/**
 * Created by caojiajun on 2025/1/20
 */
public record WalReadResult(List<LogRecord> records, long nextOffset) {
}
