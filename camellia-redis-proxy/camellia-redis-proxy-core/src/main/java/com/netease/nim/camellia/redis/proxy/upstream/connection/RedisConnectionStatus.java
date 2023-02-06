package com.netease.nim.camellia.redis.proxy.upstream.connection;

/**
 * Created by caojiajun on 2023/2/6
 */
public enum RedisConnectionStatus {

    VALID,
    INVALID,
    INITIALIZE,
    CLOSING,
    ;
}
