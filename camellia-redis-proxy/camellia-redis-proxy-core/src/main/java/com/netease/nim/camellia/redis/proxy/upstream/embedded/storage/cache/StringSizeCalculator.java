package com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.cache;

/**
 * Created by caojiajun on 2025/1/8
 */
public class StringSizeCalculator implements SizeCalculator<String> {

    public static final BytesSizeCalculator INSTANCE = new BytesSizeCalculator();

    @Override
    public long size(String element) {
        return element.length();
    }
}
