package com.netease.nim.camellia.redis.proxy.netty;

/**
 * Created by caojiajun on 2023/1/13
 */
public enum NettyTransportMode {
    nio,
    epoll,
    kqueue,
    io_uring,
    auto,
    ;
}
