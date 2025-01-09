package com.netease.nim.camellia.redis.proxy.upstream.local.storage.cache;

import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.EstimateSizeValue;

/**
 * Created by caojiajun on 2025/1/8
 */
public class EstimateSizeValueCalculator<T extends EstimateSizeValue> implements SizeCalculator<T> {

    @Override
    public long size(T element) {
        return element.estimateSize();
    }
}
