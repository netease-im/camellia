package com.netease.nim.camellia.redis.proxy.command;

import com.netease.nim.camellia.redis.proxy.netty.ChannelInfo;
import io.netty.channel.ChannelHandlerContext;

import java.util.List;

/**
 *
 * Created by caojiajun on 2019/11/11.
 */
public interface CommandInvoker {

    void invoke(ChannelHandlerContext ctx, ChannelInfo channelInfo, List<Command> commands);
}
