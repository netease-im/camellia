package com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.cache;

/**
 * Created by caojiajun on 2025/1/8
 */
public interface SizeCalculator<T> {

    StringSizeCalculator STRING_INSTANCE = new StringSizeCalculator();
    BytesSizeCalculator BYTES_INSTANCE = new BytesSizeCalculator();

    long size(T element);
}
