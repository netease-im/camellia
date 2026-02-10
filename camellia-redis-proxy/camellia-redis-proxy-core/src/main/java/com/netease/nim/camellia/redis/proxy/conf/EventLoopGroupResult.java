package com.netease.nim.camellia.redis.proxy.conf;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;

/**
 * Created by caojiajun on 2026/2/10
 */
public record EventLoopGroupResult(EventLoopGroup bossGroup, EventLoopGroup workGroup,
                                   int bossThread, int workThread, Class<? extends ServerChannel> serverChannelClass) {
}
