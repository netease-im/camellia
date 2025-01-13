package com.netease.nim.camellia.redis.proxy.upstream.local.storage.value.persist;

import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.Key;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.KeyInfo;

import java.util.Map;

/**
 * Created by caojiajun on 2025/1/6
 */
public record StringValueFlushTask(short slot, Map<Key, byte[]> flushValues, Map<Key, KeyInfo> keyMap) {
}
