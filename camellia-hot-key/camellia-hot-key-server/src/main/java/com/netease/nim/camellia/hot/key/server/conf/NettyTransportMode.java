package com.netease.nim.camellia.hot.key.server.conf;


public enum NettyTransportMode {
    nio,
    epoll,
    kqueue,
    io_uring,
    auto,
    ;
}
