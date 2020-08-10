package com.netease.nim.camellia.redis.proxy.command.async;

import com.netease.nim.camellia.redis.exception.CamelliaRedisException;
import com.netease.nim.camellia.redis.proxy.command.*;
import com.netease.nim.camellia.redis.proxy.conf.CamelliaServerProperties;
import com.netease.nim.camellia.redis.proxy.conf.CamelliaTranspondProperties;
import com.netease.nim.camellia.redis.proxy.netty.ChannelInfo;
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
 * Created by caojiajun on 2019/12/12.
 */
public class AsyncCommandInvoker implements CommandInvoker {

    private static final Logger logger = LoggerFactory.getLogger(AsyncCommandInvoker.class);

    private final AsyncCamelliaRedisTemplateChooser chooser;
    private final boolean commandSpendTimeMonitorEnable;
    private final long slowCommandThresholdMillisTime;
    private CommandFilter commandFilter = null;

    public AsyncCommandInvoker(CamelliaServerProperties serverProperties, CamelliaTranspondProperties transpondProperties) {
        this.slowCommandThresholdMillisTime = serverProperties.getSlowCommandThresholdMillisTime();
        this.commandSpendTimeMonitorEnable = serverProperties.isMonitorEnable() && serverProperties.isCommandSpendTimeMonitorEnable();
        this.chooser = new AsyncCamelliaRedisTemplateChooser(transpondProperties);
        String commandFilterClassName = serverProperties.getCommandFilterClassName();
        if (commandFilterClassName != null) {
            try {
                Class<?> clazz = Class.forName(commandFilterClassName);
                commandFilter = (CommandFilter) clazz.newInstance();
                logger.info("init CommandFilter success, CommandFilter = {}", clazz);
            } catch (Exception e) {
                logger.error("init CommandFilter error, class = {}", commandFilterClassName, e);
                throw new CamelliaRedisException(e);
            }
        }
    }

    @Override
    public void invoke(ChannelHandlerContext ctx, ChannelInfo channelInfo, List<Command> commands) {
        if (commands.isEmpty()) return;
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

            boolean needFilter = commandFilter != null;
            List<Command> list = null;
            if (needFilter) {
                list = new ArrayList<>();
            }

            for (Command command : commands) {
                AsyncTask task = new AsyncTask(taskQueue, command, commandSpendTimeMonitorEnable, slowCommandThresholdMillisTime);
                boolean add = taskQueue.add(task);
                if (!add) {
                    taskQueue.clear();
                    logger.warn("AsyncTaskQueue full, client connect will be disconnect, consid = {}", channelInfo.getConsid());
                    ctx.writeAndFlush(ErrorReply.TOO_BUSY).addListener((ChannelFutureListener) future -> ctx.close());
                    return;
                }
                tasks.add(task);

                if (needFilter) {
                    CommandFilterResponse response;
                    try {
                        response = commandFilter.check(command);
                    } catch (Exception e) {
                        String errorMsg = "ERR command filter error [" + e.getMessage() + "]";
                        ErrorLogCollector.collect(AsyncCommandInvoker.class, errorMsg);
                        response = new CommandFilterResponse(false, errorMsg);
                    }
                    if (!response.isPass()) {
                        String errorMsg = response.getErrorMsg();
                        if (errorMsg == null) {
                            errorMsg = CommandFilterResponse.DEFAULT_FAIL.getErrorMsg();
                        }
                        task.replyCompleted(new ErrorReply(errorMsg));
                    } else {
                        list.add(command);
                    }
                }
            }

            if (needFilter) {
                commands = list;
                if (commands.isEmpty()) {
                    return;
                }
            }

            AsyncCamelliaRedisTemplate template = channelInfo.getTemplate();
            if (template == null) {
                try {
                    template = chooser.choose(channelInfo);
                } catch (Exception e) {
                    logger.error("AsyncCamelliaRedisTemplateChooser choose error, bid = {}, bgroup = {}",
                            ClientCommandUtil.getBid(channelInfo), ClientCommandUtil.getBgroup(channelInfo), e);
                }
            }
            if (template == null) {
                for (int i = 0; i < commands.size(); i++) {
                    Command command = commands.get(i);
                    logger.warn("AsyncCamelliaRedisTemplate choose fail, command return NOT_AVAILABLE, consid = {}, bid = {}, bgroup = {}, command = {}",
                            channelInfo.getConsid(), ClientCommandUtil.getBid(channelInfo), ClientCommandUtil.getBgroup(channelInfo), command.getName());
                    AsyncTask task = tasks.get(i);
                    task.replyCompleted(ErrorReply.NOT_AVAILABLE);
                }
            } else {
                channelInfo.setTemplate(template);
                List<CompletableFuture<Reply>> futureList = template.sendCommand(commands);
                for (int i = 0; i < commands.size(); i++) {
                    AsyncTask task = tasks.get(i);
                    CompletableFuture<Reply> completableFuture = futureList.get(i);
                    completableFuture.thenAccept(task::replyCompleted);
                }
            }
        } catch (Exception e) {
            ctx.close();
            logger.error("AsyncCommandInvoker error", e);
        }
    }
}
