package com.netease.nim.camellia.redis.proxy.command.async;

import com.netease.nim.camellia.redis.proxy.command.AuthCommandProcessor;
import com.netease.nim.camellia.redis.proxy.command.ClientCommandUtil;
import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.command.HelloCommandUtil;
import com.netease.nim.camellia.redis.proxy.command.async.bigkey.BigKeyHunter;
import com.netease.nim.camellia.redis.proxy.command.async.connectlimit.ConnectLimiterHolder;
import com.netease.nim.camellia.redis.proxy.command.async.converter.Converters;
import com.netease.nim.camellia.redis.proxy.command.async.hotkey.HotKeyHunter;
import com.netease.nim.camellia.redis.proxy.command.async.hotkey.HotKeyHunterManager;
import com.netease.nim.camellia.redis.proxy.command.async.hotkeycache.HotKeyCache;
import com.netease.nim.camellia.redis.proxy.command.async.hotkeycache.HotKeyCacheManager;
import com.netease.nim.camellia.redis.proxy.command.async.hotkeycache.HotValue;
import com.netease.nim.camellia.redis.proxy.command.async.info.ProxyInfoUtils;
import com.netease.nim.camellia.redis.proxy.command.async.interceptor.CommandInterceptResponse;
import com.netease.nim.camellia.redis.proxy.command.async.interceptor.CommandInterceptor;
import com.netease.nim.camellia.redis.proxy.command.async.spendtime.CommandSpendTimeConfig;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.monitor.ChannelMonitor;
import com.netease.nim.camellia.redis.proxy.monitor.RedisMonitor;
import com.netease.nim.camellia.redis.proxy.netty.ChannelInfo;
import com.netease.nim.camellia.redis.proxy.reply.BulkReply;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.reply.StatusReply;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 *
 * Created by caojiajun on 2021/5/26
 */
public class CommandsTransponder {

    private static final Logger logger = LoggerFactory.getLogger(CommandsTransponder.class);

    private final AuthCommandProcessor authCommandProcessor;
    private final AsyncCamelliaRedisTemplateChooser chooser;

    private final CommandSpendTimeConfig commandSpendTimeConfig;
    private final CommandInterceptor commandInterceptor;
    private final HotKeyHunterManager hotKeyHunterManager;
    private final HotKeyCacheManager hotKeyCacheManager;
    private final BigKeyHunter bigKeyHunter;
    private final Converters converters;
    private boolean eventLoopSetSuccess = false;

    public CommandsTransponder(AsyncCamelliaRedisTemplateChooser chooser, CommandInvokeConfig commandInvokeConfig) {
        this.chooser = chooser;
        this.authCommandProcessor = commandInvokeConfig.getAuthCommandProcessor();
        this.commandSpendTimeConfig = commandInvokeConfig.getCommandSpendTimeConfig();
        this.commandInterceptor = commandInvokeConfig.getCommandInterceptor();
        this.hotKeyHunterManager = commandInvokeConfig.getHotKeyHunterManager();
        this.hotKeyCacheManager = commandInvokeConfig.getHotKeyCacheManager();
        this.bigKeyHunter = commandInvokeConfig.getBigKeyHunter();
        this.converters = commandInvokeConfig.getConverters();
    }

    public void transpond(ChannelInfo channelInfo, List<Command> commands) {
        if (!eventLoopSetSuccess) {
            RedisClientHub.updateEventLoop(channelInfo.getCtx().channel().eventLoop());
            eventLoopSetSuccess = true;
        }
        try {
            boolean hasCommandsSkip = false;
            AsyncTaskQueue taskQueue = channelInfo.getAsyncTaskQueue();

            if (logger.isDebugEnabled()) {
                List<String> commandNameList = new ArrayList<>(commands.size());
                for (Command command : commands) {
                    commandNameList.add(command.getName());
                }
                logger.debug("receive commands, commands.size = {}, consid = {}, commands = {}",
                        commands.size(), taskQueue.getChannelInfo().getConsid(), commandNameList);
            }

            List<AsyncTask> tasks = new ArrayList<>(commands.size());
            ChannelHandlerContext ctx = channelInfo.getCtx();

            boolean needIntercept = commandInterceptor != null;

            for (Command command : commands) {
                //设置channelInfo
                command.setChannelInfo(channelInfo);
                //监控
                if (RedisMonitor.isMonitorEnable()) {
                    Long bid = channelInfo.getBid();
                    String bgroup = channelInfo.getBgroup();
                    RedisMonitor.incr(bid, bgroup, command.getName());
                }

                //热key监控
                if (hotKeyHunterManager != null) {
                    HotKeyHunter hotKeyHunter = hotKeyHunterManager.get(channelInfo.getBid(), channelInfo.getBgroup());
                    if (hotKeyHunter != null) {
                        try {
                            List<byte[]> keys = command.getKeys();
                            if (keys != null) {
                                hotKeyHunter.incr(keys);
                            }
                        } catch (Exception e) {
                            ErrorLogCollector.collect(CommandsTransponder.class, "hot key hunter error", e);
                        }
                    }
                }
                //key/value转换
                if (converters != null) {
                    try {
                        converters.convertRequest(command);
                    } catch (Exception e) {
                        ErrorLogCollector.collect(CommandsTransponder.class, "convert request error", e);
                    }
                }
                //任务队列
                AsyncTask task = new AsyncTask(taskQueue, command, commandSpendTimeConfig, bigKeyHunter, converters);
                boolean add = taskQueue.add(task);
                if (!add) {
                    taskQueue.clear();
                    logger.warn("AsyncTaskQueue full, client connect will be disconnect, remote.ip = {}", ctx.channel().remoteAddress());
                    ctx.writeAndFlush(ErrorReply.TOO_BUSY).addListener((ChannelFutureListener) future -> ctx.close());
                    return;
                }
                //命令拦截器
                if (needIntercept) {
                    CommandInterceptResponse response;
                    try {
                        response = commandInterceptor.check(command);
                    } catch (Exception e) {
                        String errorMsg = "ERR command intercept error [" + e.getMessage() + "]";
                        ErrorLogCollector.collect(CommandsTransponder.class, errorMsg, e);
                        response = new CommandInterceptResponse(false, errorMsg);
                    }
                    if (!response.isPass()) {
                        String errorMsg = response.getErrorMsg();
                        if (errorMsg == null) {
                            errorMsg = CommandInterceptResponse.DEFAULT_FAIL.getErrorMsg();
                        }
                        task.replyCompleted(new ErrorReply(errorMsg));
                        hasCommandsSkip = true;
                        continue;
                    }
                }

                RedisCommand redisCommand = command.getRedisCommand();

                if (redisCommand == RedisCommand.PING) {
                    task.replyCompleted(StatusReply.PONG);
                    hasCommandsSkip = true;
                    continue;
                }

                if (redisCommand == RedisCommand.AUTH) {
                    boolean hasBidBgroup = channelInfo.getBid() != null && channelInfo.getBgroup() != null;
                    Reply reply = authCommandProcessor.invokeAuthCommand(channelInfo, command);
                    if (!hasBidBgroup) {
                        boolean pass = checkConnectLimit(channelInfo);
                        if (!pass) return;
                    }
                    task.replyCompleted(reply);
                    hasCommandsSkip = true;
                    continue;
                }

                if (redisCommand == RedisCommand.HELLO) {
                    boolean hasBidBgroup = channelInfo.getBid() != null && channelInfo.getBgroup() != null;
                    Reply reply = HelloCommandUtil.invokeHelloCommand(channelInfo, authCommandProcessor, command);
                    if (!hasBidBgroup) {
                        boolean pass = checkConnectLimit(channelInfo);
                        if (!pass) return;
                    }
                    task.replyCompleted(reply);
                    hasCommandsSkip = true;
                    continue;
                }

                //如果需要密码，但是没有auth，则返回NO_AUTH
                if (authCommandProcessor.isPasswordRequired()) {
                    if (channelInfo.getChannelStats() != ChannelInfo.ChannelStats.AUTH_OK) {
                        task.replyCompleted(ErrorReply.NO_AUTH);
                        hasCommandsSkip = true;
                        continue;
                    }
                }

                if (redisCommand == RedisCommand.SELECT) {
                    byte[][] objects = command.getObjects();
                    if (objects.length == 2) {
                        if ("0".equals(Utils.bytesToString(command.getObjects()[1]))) {
                            task.replyCompleted(StatusReply.OK);
                        } else {
                            task.replyCompleted(new ErrorReply("ERR DB index is out of range"));
                        }
                    } else {
                        task.replyCompleted(ErrorReply.argNumWrong(redisCommand));
                    }
                    hasCommandsSkip = true;
                    continue;
                }

                if (redisCommand == RedisCommand.INFO) {
                    CompletableFuture<Reply> future = ProxyInfoUtils.getInfoReply(command, chooser);
                    future.thenAccept(task::replyCompleted);
                    hasCommandsSkip = true;
                    continue;
                }

                if (redisCommand == RedisCommand.CLIENT) {
                    boolean hasBidBgroup = channelInfo.getBid() != null && channelInfo.getBgroup() != null;
                    Reply reply = ClientCommandUtil.invokeClientCommand(channelInfo, command);
                    if (!hasBidBgroup) {
                        boolean pass = checkConnectLimit(channelInfo);
                        if (!pass) return;
                    }
                    task.replyCompleted(reply);
                    hasCommandsSkip = true;
                    continue;
                }

                if (redisCommand == RedisCommand.QUIT) {
                    channelInfo.getCtx().close();
                    return;
                }

                if (redisCommand == RedisCommand.SUBSCRIBE || redisCommand == RedisCommand.PSUBSCRIBE) {
                    channelInfo.setInSubscribe(true);
                }
                if (channelInfo.isInSubscribe()) {
                    if (redisCommand != RedisCommand.SUBSCRIBE && redisCommand != RedisCommand.PSUBSCRIBE
                            && redisCommand != RedisCommand.UNSUBSCRIBE && redisCommand != RedisCommand.PUNSUBSCRIBE) {
                        taskQueue.reply(new ErrorReply("Command " + redisCommand.strRaw() + " not allowed while subscribed. Allowed commands are: [PSUBSCRIBE, QUIT, PUNSUBSCRIBE, SUBSCRIBE, UNSUBSCRIBE]"));
                        hasCommandsSkip = true;
                        continue;
                    }
                }

                if (redisCommand == null || redisCommand.getSupportType() == RedisCommand.CommandSupportType.NOT_SUPPORT) {
                    task.replyCompleted(ErrorReply.NOT_SUPPORT);
                    hasCommandsSkip = true;
                    continue;
                }

                //热key缓存
                if (command.getRedisCommand() == RedisCommand.GET && hotKeyCacheManager != null) {
                    if (command.getObjects().length >= 2) {
                        byte[] key = command.getObjects()[1];
                        HotKeyCache hotKeyCache = hotKeyCacheManager.get(channelInfo.getBid(), channelInfo.getBgroup());
                        if (hotKeyCache != null) {
                            task.setHotKeyCache(hotKeyCache);
                            HotValue cache = hotKeyCache.getCache(key);
                            if (cache != null) {
                                task.replyCompleted(new BulkReply(cache.getValue()), true);
                                hasCommandsSkip = true;
                                continue;
                            }
                        }
                    }
                }

                //大key监控
                if (bigKeyHunter != null) {
                    try {
                        bigKeyHunter.checkRequest(command);
                    } catch (Exception e) {
                        ErrorLogCollector.collect(CommandsTransponder.class, e.getMessage(), e);
                    }
                }
                tasks.add(task);
            }
            if (tasks.isEmpty()) return;
            if (hasCommandsSkip) {
                commands = new ArrayList<>(tasks.size());
                for (AsyncTask asyncTask : tasks) {
                    commands.add(asyncTask.getCommand());
                }
            }
            flush(channelInfo.getBid(), channelInfo.getBgroup(), tasks, commands);
        } catch (Exception e) {
            logger.error("commands transponder error, client connect will be force closed, bid = {}, bgroup = {}, addr = {}",
                    channelInfo.getBid(), channelInfo.getBgroup(), channelInfo.getCtx().channel().remoteAddress(), e);
            channelInfo.getCtx().close();
        }
    }

    private boolean checkConnectLimit(ChannelInfo channelInfo) {
        try {
            if (ConnectLimiterHolder.connectLimiter == null) return true;
            Long bid = channelInfo.getBid();
            String bgroup = channelInfo.getBgroup();
            if (bid != null && bgroup != null) {
                int currentConnect = ChannelMonitor.bidBgroupConnect(bid, bgroup);
                int threshold = ConnectLimiterHolder.connectLimiter.connectThreshold(bid, bgroup);
                if (currentConnect >= threshold) {
                    ChannelHandlerContext ctx = channelInfo.getCtx();
                    ctx.writeAndFlush(ErrorReply.TOO_MANY_CONNECTS)
                            .addListener((ChannelFutureListener) future -> ctx.close());
                    logger.warn("too many connects, connect will be force closed, bid = {}, bgroup = {}, current = {}, max = {}, consid = {}, client.addr = {}",
                            bid, bgroup, currentConnect, threshold, channelInfo.getConsid(), channelInfo.getCtx().channel().remoteAddress());
                    return false;
                }
                ChannelMonitor.initBidBgroup(bid, bgroup, channelInfo);
            }
            return true;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return true;
        }
    }

    private void flush(Long bid, String bgroup, List<AsyncTask> tasks, List<Command> commands) {
        try {
            AsyncCamelliaRedisTemplate template = null;
            try {
                template = chooser.choose(bid, bgroup);
            } catch (Exception e) {
                String log = "AsyncCamelliaRedisTemplateChooser choose error"
                        + ", bid = " + bid + ", bgroup = " + bgroup + ", ex = " + e;
                ErrorLogCollector.collect(CommandsTransponder.class, log, e);
            }
            if (template == null) {
                for (AsyncTask task : tasks) {
                    task.replyCompleted(ErrorReply.NOT_AVAILABLE);
                }
            } else {
                List<CompletableFuture<Reply>> futureList;
                try {
                    futureList = template.sendCommand(commands);
                } catch (Exception e) {
                    String log = "AsyncCamelliaRedisTemplateChooser sendCommand error"
                            + ", bid = " + bid + ", bgroup = " + bgroup + ", ex = " + e;
                    ErrorLogCollector.collect(CommandsTransponder.class, log, e);
                    for (AsyncTask task : tasks) {
                        task.replyCompleted(ErrorReply.NOT_AVAILABLE);
                    }
                    return;
                }
                for (int i = 0; i < tasks.size(); i++) {
                    AsyncTask task = tasks.get(i);
                    CompletableFuture<Reply> completableFuture = futureList.get(i);
                    completableFuture.thenAccept(task::replyCompleted);
                }
            }
        } finally {
            tasks.clear();
        }
    }
}
