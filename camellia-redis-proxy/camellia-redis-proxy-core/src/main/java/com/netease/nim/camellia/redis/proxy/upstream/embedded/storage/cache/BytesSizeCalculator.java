package com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.cache;

/**
 * Created by caojiajun on 2025/1/8
 */
public class BytesSizeCalculator implements SizeCalculator<byte[]> {

    @Override
    public long size(byte[] element) {
        return element.length;
    }
}
