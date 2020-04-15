package com.netease.nim.camellia.redis.proxy.hbase;

import com.netease.nim.camellia.hbase.CamelliaHBaseTemplate;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.command.CommandInvoker;
import com.netease.nim.camellia.redis.proxy.netty.ChannelInfo;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.util.*;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * Created by caojiajun on 2020/02/27.
 */
public class RedisHBaseCommandInvoker implements CommandInvoker {

    private static final Logger logger = LoggerFactory.getLogger(RedisHBaseCommandInvoker.class);

    private Map<String, Method> methodMap = new HashMap<>();
    private RedisHBaseCommandProcessor processor;

    public RedisHBaseCommandInvoker(CamelliaRedisTemplate redisTemplate, CamelliaHBaseTemplate hBaseTemplate) {
        processor = new RedisHBaseCommandProcessor(redisTemplate, hBaseTemplate);
        Class<? extends IRedisHBaseCommandProcessor> clazz = IRedisHBaseCommandProcessor.class;
        CommandMethodUtil.initCommandFinderMethods(clazz, methodMap);
    }

    @Override
    public void invoke(ChannelHandlerContext ctx, ChannelInfo channelInfo, List<Command> commands) {
        if (commands.isEmpty()) return;
        for (Command command : commands) {
            try {
                Method method = methodMap.get(command.getName());
                if (method == null) {
                    logger.warn("only support zset relevant commands, return NOT_SUPPORT, command = {}, consid = {}", command.getName(), channelInfo.getConsid());
                    ctx.writeAndFlush(ErrorReply.NOT_SUPPORT);
                    debugLog(ErrorReply.NOT_SUPPORT, channelInfo);
                    return;
                }
                Reply reply = (Reply) CommandInvokerUtil.invoke(method, command, processor);
                ctx.writeAndFlush(reply);
                debugLog(reply, channelInfo);
            } catch (Throwable e) {
                Reply reply = handlerError(e, command.getName());
                ctx.writeAndFlush(reply);
                debugLog(reply, channelInfo);
            }
        }
    }

    private Reply handlerError(Throwable e, String msg) {
        e = ErrorHandlerUtil.handler(e);
        String message = ErrorHandlerUtil.redisErrorMessage(e);
        String log = "invoke error, msg = " + msg + ",e=" + e.toString();
        ErrorLogCollector.collect(RedisHBaseCommandInvoker.class, log);
        return new ErrorReply(message);
    }

    private void debugLog(Reply reply, ChannelInfo channelInfo) {
        if (logger.isDebugEnabled()) {
            logger.debug("send reply = {}, consid = {}", reply.getClass().getSimpleName(), channelInfo.getConsid());
        }
    }
}
