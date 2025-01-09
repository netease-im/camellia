package com.netease.nim.camellia.redis.proxy.upstream.local.storage.cache;

/**
 * Created by caojiajun on 2025/1/8
 */
public class StringSizeCalculator implements SizeCalculator<String> {

    @Override
    public long size(String element) {
        return element.length();
    }
}
