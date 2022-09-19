package com.netease.nim.camellia.redis.proxy.netty;

import com.netease.nim.camellia.redis.proxy.upstream.AsyncCamelliaRedisTemplateChooser;
import io.netty.channel.EventLoopGroup;

/**
 *
 * Created by caojiajun on 2021/4/2
 */
public class GlobalRedisProxyEnv {

    public static AsyncCamelliaRedisTemplateChooser chooser;

    public static int bossThread;
    public static EventLoopGroup bossGroup;

    public static int workThread;
    public static EventLoopGroup workGroup;

    public static int port;
    public static int consolePort;
}
