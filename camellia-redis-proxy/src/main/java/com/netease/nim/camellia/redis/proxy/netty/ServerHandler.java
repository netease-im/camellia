package com.netease.nim.camellia.redis.proxy.netty;

import com.netease.nim.camellia.redis.proxy.command.AuthCommandUtil;
import com.netease.nim.camellia.redis.proxy.command.ClientCommandUtil;
import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.command.CommandInvoker;
import com.netease.nim.camellia.redis.proxy.conf.CamelliaServerProperties;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.monitor.MonitorCallback;
import com.netease.nim.camellia.redis.proxy.monitor.RedisMonitor;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.reply.StatusReply;
import com.netease.nim.camellia.redis.proxy.util.ConfigInitUtil;
import com.netease.nim.camellia.redis.proxy.util.Utils;
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
    private final CamelliaServerProperties properties;
    private final AuthCommandUtil authCommandUtil;

    public ServerHandler(CamelliaServerProperties properties, CommandInvoker invoker) {
        super();
        this.invoker = invoker;
        this.properties = properties;
        if (properties.isMonitorEnable()) {
            MonitorCallback monitorCallback = ConfigInitUtil.initMonitorCallback(properties);
            RedisMonitor.init(properties.getMonitorIntervalSeconds(), properties.isCommandSpendTimeMonitorEnable(), monitorCallback);
        }
        authCommandUtil = new AuthCommandUtil(ConfigInitUtil.initClientAuthProvider(properties));
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, List<Command> commandList) {
        try {
            ServerStatus.updateLastUseTime();
            ChannelInfo channelInfo = ChannelInfo.get(ctx);

            int index = -1;
            int startIndex = -1;
            for (Command command : commandList) {
                index++;
                //监控
                if (properties.isMonitorEnable()) {
                    Long bid = channelInfo.getBid();
                    String bgroup = channelInfo.getBgroup();
                    RedisMonitor.incr(bid, bgroup, command.getName());
                }

                RedisCommand redisCommand = command.getRedisCommand();

                //鉴权
                if (redisCommand == RedisCommand.AUTH) {
                    Reply reply = authCommandUtil.invokeAuthCommand(channelInfo, command);
                    ctx.writeAndFlush(reply);
                    continue;
                }

                //如果需要密码，则后续的操作都需要连接处于密码已经校验的状态
                if (properties.getPassword() != null) {
                    if (channelInfo.getChannelStats() != ChannelInfo.ChannelStats.AUTH_OK) {
                        ctx.writeAndFlush(ErrorReply.NO_AUTH);
                        continue;
                    }
                }

                //退出
                if (redisCommand == RedisCommand.QUIT) {
                    ctx.close();
                    return;
                }

                //特殊处理client命令
                if (redisCommand == RedisCommand.CLIENT) {
                    //如果还未设置过Bid，则不允许和client命令同时提交一批命令，并且还在client命令之前
                    if (startIndex >= 0 && channelInfo.getBid() == null) {
                        ctx.writeAndFlush(ErrorReply.SYNTAX_ERROR).addListener(future -> ctx.close());
                        return;
                    }
                    Reply reply = ClientCommandUtil.invokeClientCommand(channelInfo, command);
                    ctx.writeAndFlush(reply);
                    continue;
                }
                command.setChannelInfo(channelInfo);
                if (startIndex < 0) {
                    startIndex = index;
                }
            }
            //减少内存拷贝
            if (startIndex == 0) {
                invoker.invoke(ctx, channelInfo, commandList);
            } else {
                if (startIndex > 0) {
                    List<Command> commands = commandList.subList(startIndex, commandList.size());
                    invoker.invoke(ctx, channelInfo, commands);
                }
            }
        } catch (Exception e) {
            ctx.close();
            logger.error("error", e);
        }
    }
}
