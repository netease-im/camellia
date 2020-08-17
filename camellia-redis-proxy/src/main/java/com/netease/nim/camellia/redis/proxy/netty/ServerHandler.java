package com.netease.nim.camellia.redis.proxy.netty;

import com.netease.nim.camellia.redis.proxy.command.ClientCommandUtil;
import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.command.CommandInvoker;
import com.netease.nim.camellia.redis.proxy.conf.CamelliaServerProperties;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.monitor.RedisMonitor;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.reply.StatusReply;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;


@ChannelHandler.Sharable
public class ServerHandler extends SimpleChannelInboundHandler<List<Command>> {

    private static final Logger logger = LoggerFactory.getLogger(ServerHandler.class);

    private final CommandInvoker invoker;
    private final CamelliaServerProperties env;

    public ServerHandler(CamelliaServerProperties env, CommandInvoker invoker) {
        super();
        this.invoker = invoker;
        this.env = env;
        if (env.isMonitorEnable()) {
            RedisMonitor.init(env.getMonitorIntervalSeconds(), env.isCommandSpendTimeMonitorEnable());
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, List<Command> commandList) {
        try {
            ServerStatus.updateLastUseTime();
            ChannelInfo channelInfo = ChannelInfo.get(ctx);

            List<Command> commands = new ArrayList<>();
            for (Command command : commandList) {
                //监控
                if (env.isMonitorEnable()) {
                    Long bid = channelInfo.getBid();
                    String bgroup = channelInfo.getBgroup();
                    RedisMonitor.incr(bid, bgroup, command.getName());
                }

                RedisCommand redisCommand = command.getRedisCommand();

                //鉴权
                if (redisCommand == RedisCommand.AUTH) {
                    if (env.getPassword() == null) {
                        ctx.writeAndFlush(new ErrorReply("ERR Client sent AUTH, but no password is set"));
                        continue;
                    } else {
                        byte[][] objects = command.getObjects();
                        if (objects.length != 2) {
                            ctx.writeAndFlush(ErrorReply.INVALID_PASSWORD);
                            continue;
                        }
                        String password = Utils.bytesToString(objects[1]);
                        if (password.equals(env.getPassword())) {
                            channelInfo.setChannelStats(ChannelInfo.ChannelStats.AUTH_OK);
                            ctx.writeAndFlush(StatusReply.OK);
                            continue;
                        } else {
                            channelInfo.setChannelStats(ChannelInfo.ChannelStats.NO_AUTH);
                            ctx.writeAndFlush(ErrorReply.INVALID_PASSWORD);
                            continue;
                        }
                    }
                }

                //如果需要密码，则后续的操作都需要连接处于密码已经校验的状态
                if (env.getPassword() != null) {
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
                    Reply reply = ClientCommandUtil.invokeClientCommand(channelInfo, command);
                    ctx.writeAndFlush(reply);
                    continue;
                }

                commands.add(command);
            }
            if (!commands.isEmpty()) {
                invoker.invoke(ctx, channelInfo, commands);
            }
        } catch (Exception e) {
            ctx.close();
            logger.error("error", e);
        }
    }
}
