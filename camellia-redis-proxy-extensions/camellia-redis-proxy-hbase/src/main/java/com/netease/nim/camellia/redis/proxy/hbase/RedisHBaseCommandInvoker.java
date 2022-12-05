package com.netease.nim.camellia.redis.proxy.hbase;

import com.netease.nim.camellia.core.util.ExceptionUtils;
import com.netease.nim.camellia.hbase.CamelliaHBaseTemplate;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import com.netease.nim.camellia.redis.proxy.auth.AuthCommandProcessor;
import com.netease.nim.camellia.redis.proxy.command.AsyncTask;
import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.command.CommandInvoker;
import com.netease.nim.camellia.redis.proxy.command.CommandsTransponder;
import com.netease.nim.camellia.redis.proxy.conf.CamelliaServerProperties;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.monitor.*;
import com.netease.nim.camellia.redis.proxy.netty.ChannelInfo;
import com.netease.nim.camellia.redis.proxy.plugin.*;
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

    private final Map<String, Method> methodMap = new HashMap<>();
    private final RedisHBaseCommandProcessor processor;
    private final AuthCommandProcessor authCommandProcessor;
    private final DefaultProxyPluginFactory proxyPluginFactory;
    private ProxyPluginInitResp proxyPluginInitResp;

    public RedisHBaseCommandInvoker(CamelliaRedisTemplate redisTemplate, CamelliaHBaseTemplate hBaseTemplate, CamelliaServerProperties serverProperties) {
        ProxyDynamicConf.updateInitConf(serverProperties.getConfig());

        processor = new RedisHBaseCommandProcessor(redisTemplate, hBaseTemplate);

        MonitorCallback monitorCallback = ConfigInitUtil.initMonitorCallback(serverProperties);
        ProxyMonitorCollector.init(serverProperties, monitorCallback);

        Class<? extends IRedisHBaseCommandProcessor> clazz = IRedisHBaseCommandProcessor.class;
        CommandMethodUtil.initCommandFinderMethods(clazz, methodMap);

        this.authCommandProcessor = new AuthCommandProcessor(ConfigInitUtil.initClientAuthProvider(serverProperties));
        this.proxyPluginFactory = new DefaultProxyPluginFactory(serverProperties.getPlugins(), serverProperties.getProxyBeanFactory());
        this.proxyPluginInitResp = proxyPluginFactory.initPlugins();
        proxyPluginFactory.registerPluginUpdate(() -> proxyPluginInitResp = proxyPluginFactory.initPlugins());
    }

    @Override
    public void invoke(ChannelHandlerContext ctx, ChannelInfo channelInfo, List<Command> commands) {
        if (commands.isEmpty()) return;
        for (Command command : commands) {
            Reply reply;
            try {
                boolean requestPluginPass = executeRequest(ctx, channelInfo, command);
                if (!requestPluginPass) {
                    continue;
                }
                if (command.getRedisCommand() == RedisCommand.AUTH) {
                    reply = authCommandProcessor.invokeAuthCommand(channelInfo, command);
                } else if (command.getRedisCommand() == RedisCommand.QUIT) {
                    ctx.close();
                    return;
                } else {
                    if (authCommandProcessor.isPasswordRequired() && channelInfo.getChannelStats() == ChannelInfo.ChannelStats.NO_AUTH) {
                        reply = ErrorReply.NO_AUTH;
                    } else {
                        Method method = methodMap.get(command.getName());
                        if (method == null) {
                            logger.warn("only support zset relevant commands, return NOT_SUPPORT, command = {}, consid = {}", command.getName(), channelInfo.getConsid());
                            reply = ErrorReply.NOT_SUPPORT;
                        } else {
                            //invoke start
                            reply = (Reply) CommandInvokerUtil.invoke(method, command, processor);
                            //invoke end
                        }
                    }
                }
                if (executeReply(ctx, channelInfo, command, reply, false)) {
                    ctx.writeAndFlush(reply);
                    debugLog(reply, channelInfo);
                }
            } catch (Throwable e) {
                reply = handlerError(e, command.getName());
                if (executeReply(ctx, channelInfo, command, reply, false)) {
                    ctx.writeAndFlush(reply);
                    debugLog(reply, channelInfo);
                }
            }
        }
    }

    private boolean executeRequest(ChannelHandlerContext ctx, ChannelInfo channelInfo, Command command) {
        List<ProxyPlugin> requestPlugins = proxyPluginInitResp.getRequestPlugins();
        if (!requestPlugins.isEmpty()) {
            ProxyRequest request = new ProxyRequest(command, null);
            for (ProxyPlugin plugin : requestPlugins) {
                try {
                    ProxyPluginResponse response = plugin.executeRequest(request);
                    if (!response.isPass()) {
                        Reply reply = response.getReply();
                        if (executeReply(ctx, channelInfo, command, reply, true)) {
                            ctx.writeAndFlush(reply);
                            debugLog(reply, channelInfo);
                        }
                        return false;
                    }
                } catch (Exception e) {
                    ErrorLogCollector.collect(CommandsTransponder.class, "executeRequest error", e);
                }
            }
        }
        return true;
    }

    private boolean executeReply(ChannelHandlerContext ctx, ChannelInfo channelInfo, Command command, Reply reply, boolean fromPlugin) {
        List<ProxyPlugin> replyPlugins = proxyPluginInitResp.getReplyPlugins();
        if (!replyPlugins.isEmpty()) {
            ProxyReply proxyReply = new ProxyReply(command, reply, fromPlugin);
            for (ProxyPlugin plugin : replyPlugins) {
                try {
                    ProxyPluginResponse response = plugin.executeReply(proxyReply);
                    if (!response.isPass()) {
                        ctx.writeAndFlush(reply);
                        debugLog(reply, channelInfo);
                        return false;
                    }
                } catch (Exception e) {
                    ErrorLogCollector.collect(AsyncTask.class, "executeReply error", e);
                }
            }
        }
        return true;
    }

    private Reply handlerError(Throwable e, String msg) {
        e = ExceptionUtils.onError(e);
        String message = ErrorHandlerUtil.redisErrorMessage(e);
        String log = "invoke error, msg = " + msg + ",e=" + e;
        ErrorLogCollector.collect(RedisHBaseCommandInvoker.class, log);
        return new ErrorReply(message);
    }

    private void debugLog(Reply reply, ChannelInfo channelInfo) {
        if (logger.isDebugEnabled()) {
            logger.debug("send reply = {}, consid = {}", reply.getClass().getSimpleName(), channelInfo.getConsid());
        }
    }
}
