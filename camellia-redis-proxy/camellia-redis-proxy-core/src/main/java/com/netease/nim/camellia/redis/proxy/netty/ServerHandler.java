package com.netease.nim.camellia.redis.proxy.netty;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.command.ICommandInvoker;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


@ChannelHandler.Sharable
public class ServerHandler extends SimpleChannelInboundHandler<List<Command>> {

    private static final Logger logger = LoggerFactory.getLogger(ServerHandler.class);

    private final ICommandInvoker invoker;

    public ServerHandler(ICommandInvoker invoker) {
        super();
        this.invoker = invoker;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, List<Command> commandList) {
        ChannelInfo channelInfo = ChannelInfo.get(ctx);
        try {
            if (!channelInfo.isFromCport()) {
                ServerStatus.updateLastUseTime();
            }
            ReplyFlushEncoder.commandReceive(ctx, commandList.size());
            invoker.invoke(ctx, channelInfo, commandList);
        } catch (Exception e) {
            ctx.close();
            logger.error("error", e);
        }
    }
}
