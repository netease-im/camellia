package com.netease.nim.camellia.redis.proxy.command.async;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.monitor.RedisMonitor;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * Created by caojiajun on 2019/12/12.
 */
public class AsyncTask {

    private static final Logger logger = LoggerFactory.getLogger(AsyncTask.class);
    private static final Logger slowCommandLogger = LoggerFactory.getLogger("slowCommandStats");

    private final AsyncTaskQueue taskQueue;
    private final Command command;
    private final boolean commandSpendTimeMonitorEnable;
    private final long slowCommandThresholdMillisTime;
    private long startTime;
    private Reply reply;

    public AsyncTask(AsyncTaskQueue taskQueue, Command command, boolean commandSpendTimeMonitorEnable, long slowCommandThresholdMillisTime) {
        this.command = command;
        this.taskQueue = taskQueue;
        this.commandSpendTimeMonitorEnable = commandSpendTimeMonitorEnable;
        this.slowCommandThresholdMillisTime = slowCommandThresholdMillisTime;
        if (this.commandSpendTimeMonitorEnable) {
            startTime = System.nanoTime();
        }
    }

    public void replyCompleted(Reply reply) {
        if (commandSpendTimeMonitorEnable) {
            long spendNanoTime = System.nanoTime() - startTime;
            if (spendNanoTime > slowCommandThresholdMillisTime * 1000000L) {
                slowCommandLogger.warn("slow command, spendMs = {}, command = {}, params = {}", spendNanoTime / 1000000.0, command.getName(), JSONObject.toJSON(command.getObjects()));
            }
            RedisMonitor.incrCommandSpendTime(command.getName(), spendNanoTime);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("AsyncTask replyCompleted, reply = {}, consid = {}", reply.getClass().getSimpleName(), taskQueue.getChannelInfo().getConsid());
        }
        this.reply = reply;
        this.taskQueue.callback();
    }

    public Command getCommand() {
        return command;
    }

    public Reply getReply() {
        return reply;
    }
}
