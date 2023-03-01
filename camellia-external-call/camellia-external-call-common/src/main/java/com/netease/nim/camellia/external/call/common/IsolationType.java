package com.netease.nim.camellia.external.call.common;

/**
 * Created by caojiajun on 2023/2/28
 */
public enum IsolationType {
    FAST,
    SLOW,
    FAST_ERROR,
    SLOW_ERROR,
    RETRY,
    HIGH_PRIORITY_RETRY,
    ISOLATION,
    ;
}
