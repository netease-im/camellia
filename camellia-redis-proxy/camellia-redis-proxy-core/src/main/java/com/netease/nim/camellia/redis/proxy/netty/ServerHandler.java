package com.netease.nim.camellia.redis.proxy.netty;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.command.ICommandInvoker;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.epoll.EpollChannelOption;
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
            invoker.invoke(ctx, channelInfo, commandList);
        } catch (Exception e) {
            ctx.close();
            logger.error("error", e);
        } finally {
            try {
                if (GlobalRedisProxyEnv.isServerTcpQuickAckEnable() && channelInfo.getChannelType() == ChannelType.tcp) {
                    ctx.channel().config().setOption(EpollChannelOption.TCP_QUICKACK, Boolean.TRUE);
                }
            } catch (Exception e) {
                ErrorLogCollector.collect(ServerHandler.class, "set TCP_QUICKACK error", e);
            }
        }
    }
}
