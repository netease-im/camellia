package com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.persist;

import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.Key;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.KeyInfo;

import java.util.Map;

/**
 * Created by caojiajun on 2025/1/2
 */
public record KeyFlushTask(short slot, Map<Key, KeyInfo> flushKeys) {
}
