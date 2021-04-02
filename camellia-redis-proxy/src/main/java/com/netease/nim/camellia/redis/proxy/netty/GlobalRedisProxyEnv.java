package com.netease.nim.camellia.redis.proxy.netty;

import io.netty.channel.EventLoopGroup;

/**
 *
 * Created by caojiajun on 2021/4/2
 */
public class GlobalRedisProxyEnv {

    public static int bossThread;
    public static EventLoopGroup bossGroup;

    public static int workThread;
    public static EventLoopGroup workGroup;

}
