package com.netease.nim.camellia.redis.proxy.command.async;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.command.async.bigkey.BigKeyHunter;
import com.netease.nim.camellia.redis.proxy.command.async.hotkey.HotKeyHunter;
import com.netease.nim.camellia.redis.proxy.command.async.hotkey.HotKeyHunterManager;
import com.netease.nim.camellia.redis.proxy.command.async.hotkeycache.HotKeyCache;
import com.netease.nim.camellia.redis.proxy.command.async.hotkeycache.HotKeyCacheManager;
import com.netease.nim.camellia.redis.proxy.command.async.hotkeycache.HotValue;
import com.netease.nim.camellia.redis.proxy.command.async.spendtime.CommandSpendTimeConfig;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.netty.ChannelInfo;
import com.netease.nim.camellia.redis.proxy.reply.BulkReply;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
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

    private final AsyncCamelliaRedisTemplateChooser chooser;
    private final CommandSpendTimeConfig commandSpendTimeConfig;
    private final CommandInterceptor commandInterceptor;
    private final HotKeyHunterManager hotKeyHunterManager;
    private final HotKeyCacheManager hotKeyCacheManager;
    private final BigKeyHunter bigKeyHunter;
    private boolean eventLoopSetSuccess = false;

    public CommandsTransponder(AsyncCamelliaRedisTemplateChooser chooser, CommandInvokeConfig commandInvokeConfig) {
        this.chooser = chooser;
        this.commandSpendTimeConfig = commandInvokeConfig.getCommandSpendTimeConfig();
        this.commandInterceptor = commandInvokeConfig.getCommandInterceptor();
        this.hotKeyHunterManager = commandInvokeConfig.getHotKeyHunterManager();
        this.hotKeyCacheManager = commandInvokeConfig.getHotKeyCacheManager();
        this.bigKeyHunter = commandInvokeConfig.getBigKeyHunter();
    }

    public void transpond(ChannelInfo channelInfo, List<Command> commands) {
        if (!eventLoopSetSuccess) {
            RedisClientHub.updateEventLoop(channelInfo.getCtx().channel().eventLoop());
            eventLoopSetSuccess = true;
        }
        try {
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
                AsyncTask task = new AsyncTask(taskQueue, command, commandSpendTimeConfig, bigKeyHunter);
                boolean add = taskQueue.add(task);
                if (!add) {
                    taskQueue.clear();
                    logger.warn("AsyncTaskQueue full, client connect will be disconnect, remote.ip = {}", ctx.channel().remoteAddress());
                    ctx.writeAndFlush(ErrorReply.TOO_BUSY).addListener((ChannelFutureListener) future -> ctx.close());
                    return;
                }
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
                        continue;
                    }
                }

                if (command.getRedisCommand() == RedisCommand.GET && hotKeyCacheManager != null) {
                    if (command.getObjects().length >= 2) {
                        byte[] key = command.getObjects()[1];
                        HotKeyCache hotKeyCache = hotKeyCacheManager.get(channelInfo.getBid(), channelInfo.getBgroup());
                        if (hotKeyCache != null) {
                            task.setHotKeyCache(hotKeyCache);
                            HotValue cache = hotKeyCache.getCache(key);
                            if (cache != null) {
                                task.replyCompleted(new BulkReply(cache.getValue()), true);
                                continue;
                            }
                        }
                    }
                }

                if (bigKeyHunter != null) {
                    try {
                        bigKeyHunter.checkRequest(command);
                    } catch (Exception e) {
                        ErrorLogCollector.collect(CommandsTransponder.class, e.getMessage(), e);
                    }
                }
                tasks.add(task);
            }
            flush(channelInfo.getBid(), channelInfo.getBgroup(), tasks);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            channelInfo.getCtx().close();
        }
    }


    private void flush(Long bid, String bgroup, List<AsyncTask> taskList) {
        AsyncCamelliaRedisTemplate template = null;
        try {
            template = chooser.choose(bid, bgroup);
        } catch (Exception e) {
            String log = "AsyncCamelliaRedisTemplateChooser choose error"
                    + ", bid = " + bid + ", bgroup = " + bgroup + ", ex = " + e.toString();
            ErrorLogCollector.collect(CommandsTransponder.class, log, e);
        }
        if (template == null) {
            for (AsyncTask task : taskList) {
                task.replyCompleted(ErrorReply.NOT_AVAILABLE);
            }
        } else {
            List<Command> commandList = new ArrayList<>(taskList.size());
            for (AsyncTask asyncTask : taskList) {
                commandList.add(asyncTask.getCommand());
            }
            List<CompletableFuture<Reply>> futureList = template.sendCommand(commandList);
            for (int i = 0; i < taskList.size(); i++) {
                AsyncTask task = taskList.get(i);
                CompletableFuture<Reply> completableFuture = futureList.get(i);
                completableFuture.thenAccept(task::replyCompleted);
            }
        }
        taskList.clear();
    }
}
