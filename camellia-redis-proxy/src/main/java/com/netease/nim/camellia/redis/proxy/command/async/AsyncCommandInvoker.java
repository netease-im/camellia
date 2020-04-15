package com.netease.nim.camellia.redis.proxy.command.async;

import com.netease.nim.camellia.redis.proxy.command.ClientCommandUtil;
import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.command.CommandInvoker;
import com.netease.nim.camellia.redis.proxy.conf.CamelliaTranspondProperties;
import com.netease.nim.camellia.redis.proxy.netty.ChannelInfo;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
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

    private AsyncCamelliaRedisTemplateChooser chooser;

    public AsyncCommandInvoker(CamelliaTranspondProperties transpondProperties) {
        chooser = new AsyncCamelliaRedisTemplateChooser(transpondProperties);
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

            for (int i=0; i<commands.size(); i++) {
                AsyncTask task = new AsyncTask(taskQueue);
                boolean add = taskQueue.add(task);
                if (!add) {
                    taskQueue.clear();
                    logger.warn("AsyncTaskQueue full, client connect will be disconnect, consid = {}", channelInfo.getConsid());
                    ctx.writeAndFlush(ErrorReply.TOO_BUSY).addListener((ChannelFutureListener) future -> ctx.close());
                    return;
                }
                tasks.add(task);
            }
            AsyncCamelliaRedisTemplate template = null;
            try {
                template = chooser.choose(channelInfo);
            } catch (Exception e) {
                logger.error("AsyncCamelliaRedisTemplateChooser choose error, bid = {}, bgroup = {}",
                        ClientCommandUtil.getBid(channelInfo), ClientCommandUtil.getBgroup(channelInfo), e);
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
