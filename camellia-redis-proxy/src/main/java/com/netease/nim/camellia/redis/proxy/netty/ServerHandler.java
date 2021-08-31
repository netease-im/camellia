package com.netease.nim.camellia.redis.proxy.netty;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.command.CommandInvoker;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


@ChannelHandler.Sharable
public class ServerHandler extends SimpleChannelInboundHandler<List<Command>> {

    private static final Logger logger = LoggerFactory.getLogger(ServerHandler.class);

    private final CommandInvoker invoker;

    public ServerHandler(CommandInvoker invoker) {
        super();
        this.invoker = invoker;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, List<Command> commandList) {
        try {
            ServerStatus.updateLastUseTime();
            ChannelInfo channelInfo = ChannelInfo.get(ctx);
            invoker.invoke(ctx, channelInfo, commandList);
        } catch (Exception e) {
            ctx.close();
            logger.error("error", e);
        }
    }
}
