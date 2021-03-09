package com.netease.nim.camellia.redis.proxy.hbase;

import com.netease.nim.camellia.hbase.CamelliaHBaseTemplate;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.command.CommandInvoker;
import com.netease.nim.camellia.redis.proxy.command.async.bigkey.BigKeyHunter;
import com.netease.nim.camellia.redis.proxy.command.async.bigkey.CommandBigKeyMonitorConfig;
import com.netease.nim.camellia.redis.proxy.command.async.hotkey.CommandHotKeyMonitorConfig;
import com.netease.nim.camellia.redis.proxy.command.async.hotkey.HotKeyHunter;
import com.netease.nim.camellia.redis.proxy.command.async.hotkey.HotKeyHunterManager;
import com.netease.nim.camellia.redis.proxy.command.async.spendtime.CommandSpendTimeConfig;
import com.netease.nim.camellia.redis.proxy.conf.CamelliaServerProperties;
import com.netease.nim.camellia.redis.proxy.monitor.*;
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

    private final Map<String, Method> methodMap = new HashMap<>();
    private final RedisHBaseCommandProcessor processor;

    private CommandSpendTimeConfig commandSpendTimeConfig;
    private HotKeyHunter hotKeyHunter;
    private BigKeyHunter bigKeyHunter;

    public RedisHBaseCommandInvoker(CamelliaRedisTemplate redisTemplate, CamelliaHBaseTemplate hBaseTemplate, CamelliaServerProperties serverProperties) {
        processor = new RedisHBaseCommandProcessor(redisTemplate, hBaseTemplate);
        Class<? extends IRedisHBaseCommandProcessor> clazz = IRedisHBaseCommandProcessor.class;
        CommandMethodUtil.initCommandFinderMethods(clazz, methodMap);

        int monitorIntervalSeconds = serverProperties.getMonitorIntervalSeconds();
        CommandSpendTimeConfig commandSpendTimeConfig = ConfigInitUtil.initCommandSpendTimeConfig(serverProperties);
        if (commandSpendTimeConfig != null) {
            SlowCommandMonitor.init(monitorIntervalSeconds);
            this.commandSpendTimeConfig = commandSpendTimeConfig;
        }
        CommandHotKeyMonitorConfig commandHotKeyMonitorConfig = ConfigInitUtil.initCommandHotKeyMonitorConfig(serverProperties);
        if (commandHotKeyMonitorConfig != null) {
            HotKeyMonitor.init(monitorIntervalSeconds);
            HotKeyHunterManager hotKeyHunterManager = new HotKeyHunterManager(commandHotKeyMonitorConfig);
            this.hotKeyHunter = hotKeyHunterManager.get(null, null);
        }
        CommandBigKeyMonitorConfig commandBigKeyMonitorConfig = ConfigInitUtil.initBigKeyMonitorConfig(serverProperties);
        if (commandBigKeyMonitorConfig != null) {
            BigKeyMonitor.init(monitorIntervalSeconds);
            this.bigKeyHunter = new BigKeyHunter(commandBigKeyMonitorConfig);
        }
    }

    @Override
    public void invoke(ChannelHandlerContext ctx, ChannelInfo channelInfo, List<Command> commands) {
        if (commands.isEmpty()) return;
        for (Command command : commands) {
            Reply reply = null;
            long startTime = 0;
            if (RedisMonitor.isCommandSpendTimeMonitorEnable()) {
                startTime = System.nanoTime();
            }
            try {
                Method method = methodMap.get(command.getName());
                if (method == null) {
                    logger.warn("only support zset relevant commands, return NOT_SUPPORT, command = {}, consid = {}", command.getName(), channelInfo.getConsid());
                    ctx.writeAndFlush(ErrorReply.NOT_SUPPORT);
                    debugLog(ErrorReply.NOT_SUPPORT, channelInfo);
                    return;
                }
                if (hotKeyHunter != null) {
                    hotKeyHunter.incr(command.getKeys());
                }
                if (bigKeyHunter != null) {
                    bigKeyHunter.checkUpstream(command);
                }
                //
                reply = (Reply) CommandInvokerUtil.invoke(method, command, processor);
                ctx.writeAndFlush(reply);
                debugLog(reply, channelInfo);
                //
                if (bigKeyHunter != null) {
                    bigKeyHunter.checkDownstream(command, reply);
                }
            } catch (Throwable e) {
                reply = handlerError(e, command.getName());
                ctx.writeAndFlush(reply);
                debugLog(reply, channelInfo);
            } finally {
                if (startTime > 0) {
                    long spendNanoTime = System.nanoTime() - startTime;
                    RedisMonitor.incrCommandSpendTime(command.getName(), spendNanoTime);
                    if (this.commandSpendTimeConfig != null && spendNanoTime > this.commandSpendTimeConfig.getSlowCommandThresholdNanoTime()) {
                        double spendMillis = spendNanoTime / 1000000.0;
                        long slowCommandThresholdMillisTime = this.commandSpendTimeConfig.getSlowCommandThresholdMillisTime();
                        SlowCommandMonitor.slowCommand(command, spendMillis, slowCommandThresholdMillisTime);
                        if (this.commandSpendTimeConfig.getSlowCommandMonitorCallback() != null) {
                            try {
                                this.commandSpendTimeConfig.getSlowCommandMonitorCallback().callback(command, reply,
                                        spendMillis, slowCommandThresholdMillisTime);
                            } catch (Exception e) {
                                ErrorLogCollector.collect(RedisHBaseCommandInvoker.class, "SlowCommandCallback error", e);
                            }
                        }
                    }
                }
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
