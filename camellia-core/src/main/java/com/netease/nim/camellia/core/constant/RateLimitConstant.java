package com.netease.nim.camellia.core.constant;

/**
 * @author anhdt9
 * @since 21/12/2022
 */
public class RateLimitConstant {
    private RateLimitConstant() {
        throw new IllegalStateException("Utility class");
    }

    public static final long DEFAULT_CHECK_MILLIS = 1000L;
    public static final long DEFAULT_MAX_COUNT = -1L;
}
