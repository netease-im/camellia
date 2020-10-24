package com.netease.nim.camellia.redis.proxy.command.async;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.command.async.spendtime.CommandSpendTimeConfig;
import com.netease.nim.camellia.redis.proxy.monitor.RedisMonitor;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * Created by caojiajun on 2019/12/12.
 */
public class AsyncTask {

    private static final Logger logger = LoggerFactory.getLogger(AsyncTask.class);

    private final AsyncTaskQueue taskQueue;
    private final Command command;
    private final CommandSpendTimeConfig commandSpendTimeConfig;
    private long startTime;
    private Reply reply;

    public AsyncTask(AsyncTaskQueue taskQueue, Command command, CommandSpendTimeConfig commandSpendTimeConfig) {
        this.command = command;
        this.taskQueue = taskQueue;
        this.commandSpendTimeConfig = commandSpendTimeConfig;
        if (this.commandSpendTimeConfig != null) {
            startTime = System.nanoTime();
        }
    }

    public void replyCompleted(Reply reply) {
        try {
            if (commandSpendTimeConfig != null) {
                long spendNanoTime = System.nanoTime() - startTime;
                if (!command.isBlocking() && spendNanoTime > commandSpendTimeConfig.getSlowCommandThresholdMillisTime() * 1000000L) {
                    if (commandSpendTimeConfig.getSlowCommandCallback() != null) {
                        try {
                            commandSpendTimeConfig.getSlowCommandCallback().callback(taskQueue.getChannelInfo().getBid(),
                                    taskQueue.getChannelInfo().getBgroup(), command, spendNanoTime / 1000000.0);
                        } catch (Exception e) {
                            ErrorLogCollector.collect(AsyncTask.class, "SlowCommandCallback error", e);
                        }
                    }
                }
                RedisMonitor.incrCommandSpendTime(command.getName(), spendNanoTime);
            }
            if (logger.isDebugEnabled()) {
                logger.debug("AsyncTask replyCompleted, reply = {}, consid = {}", reply.getClass().getSimpleName(), taskQueue.getChannelInfo().getConsid());
            }
            this.reply = reply;
            this.taskQueue.callback();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            this.reply = new ErrorReply(e.getMessage());
            this.taskQueue.callback();
        }
    }

    public Command getCommand() {
        return command;
    }

    public Reply getReply() {
        return reply;
    }
}
