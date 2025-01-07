package com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.value.persist;

import com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.key.KeyInfo;

import java.util.Map;

/**
 * Created by caojiajun on 2025/1/6
 */
public record StringValueFlushTask(short slot, Map<KeyInfo, byte[]> flushValues) {
}
