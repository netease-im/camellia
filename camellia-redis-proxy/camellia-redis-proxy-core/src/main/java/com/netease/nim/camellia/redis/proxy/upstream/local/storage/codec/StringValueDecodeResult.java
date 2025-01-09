package com.netease.nim.camellia.redis.proxy.upstream.local.storage.codec;

import java.util.List;

/**
 * Created by caojiajun on 2025/1/9
 */
public record StringValueDecodeResult(List<byte[]> values, int remaining) {
}
