package com.netease.nim.camellia.feign.client;

import java.util.concurrent.TimeUnit;

/**
 * Created by caojiajun on 2022/3/8
 */
public class DynamicOption {

    private final DynamicValueGetter<Long> connectTimeout;
    private final DynamicValueGetter<TimeUnit> connectTimeoutUnit;
    private final DynamicValueGetter<Long> readTimeout;
    private final DynamicValueGetter<TimeUnit> readTimeoutUnit;
    private final DynamicValueGetter<Boolean> followRedirects;

    public DynamicOption(DynamicValueGetter<Long> connectTimeout, DynamicValueGetter<TimeUnit> connectTimeoutUnit,
                         DynamicValueGetter<Long> readTimeout, DynamicValueGetter<TimeUnit> readTimeoutUnit, DynamicValueGetter<Boolean> followRedirects) {
        this.connectTimeout = connectTimeout;
        this.connectTimeoutUnit = connectTimeoutUnit;
        this.readTimeout = readTimeout;
        this.readTimeoutUnit = readTimeoutUnit;
        this.followRedirects = followRedirects;
    }

    public Long getConnectTimeout() {
        if (connectTimeout == null) return null;
        return connectTimeout.get();
    }

    public TimeUnit getConnectTimeoutUnit() {
        if (connectTimeoutUnit == null) return null;
        return connectTimeoutUnit.get();
    }

    public Long getReadTimeout() {
        if (readTimeout == null) return null;
        return readTimeout.get();
    }

    public TimeUnit getReadTimeoutUnit() {
        if (readTimeoutUnit == null) return null;
        return readTimeoutUnit.get();
    }

    public Boolean isFollowRedirects() {
        if (followRedirects == null) return null;
        return followRedirects.get();
    }

    public static interface DynamicValueGetter<T> {
        T get();
    }
}
