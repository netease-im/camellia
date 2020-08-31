package com.netease.nim.camellia.redis.proxy.command.async.queue;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.netty.ChannelInfo;

import java.util.List;

/**
 * Created by caojiajun on 2020/8/27
 */
public interface CommandsEventProducer {

    boolean publishEvent(ChannelInfo channelInfo, List<Command> commands);
}
