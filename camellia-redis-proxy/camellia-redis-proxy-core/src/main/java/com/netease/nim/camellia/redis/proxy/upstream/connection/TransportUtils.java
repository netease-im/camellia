package com.netease.nim.camellia.redis.proxy.upstream.connection;

import io.netty.channel.EventLoop;

/**
 * Created by caojiajun on 2023/10/8
 */
public class TransportUtils {

    public static boolean match(EventLoop eventLoop, RedisConnectionAddr addr) {
        if (addr.getHost() != null && addr.getPort() > 0) {
            return true;
        }
        return false;
    }
}
