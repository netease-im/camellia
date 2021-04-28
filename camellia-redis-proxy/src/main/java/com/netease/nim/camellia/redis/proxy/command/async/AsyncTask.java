package com.netease.nim.camellia.redis.proxy.command.async;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.command.async.bigkey.BigKeyHunter;
import com.netease.nim.camellia.redis.proxy.command.async.hotkeycache.HotKeyCache;
import com.netease.nim.camellia.redis.proxy.command.async.spendtime.CommandSpendTimeConfig;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.monitor.RedisMonitor;
import com.netease.nim.camellia.redis.proxy.monitor.SlowCommandMonitor;
import com.netease.nim.camellia.redis.proxy.reply.BulkReply;
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
    private HotKeyCache hotKeyCache;
    private final BigKeyHunter bigKeyHunter;

    public AsyncTask(AsyncTaskQueue taskQueue, Command command,
                     CommandSpendTimeConfig commandSpendTimeConfig, BigKeyHunter bigKeyHunter) {
        this.command = command;
        this.taskQueue = taskQueue;
        this.commandSpendTimeConfig = commandSpendTimeConfig;
        if (RedisMonitor.isCommandSpendTimeMonitorEnable()) {
            startTime = System.nanoTime();
        }
        this.bigKeyHunter = bigKeyHunter;
    }

    public void setHotKeyCache(HotKeyCache hotKeyCache) {
        this.hotKeyCache = hotKeyCache;
    }

    public void replyCompleted(Reply reply, boolean fromCache) {
        try {
            if (startTime > 0) {
                long spendNanoTime = System.nanoTime() - startTime;
                RedisMonitor.incrCommandSpendTime(command.getChannelInfo().getBid(), command.getChannelInfo().getBgroup(), command.getName(), spendNanoTime);
                if (commandSpendTimeConfig != null && spendNanoTime > commandSpendTimeConfig.getSlowCommandThresholdNanoTime()
                        && !command.isBlocking()) {
                    long slowCommandThresholdMillisTime = commandSpendTimeConfig.getSlowCommandThresholdMillisTime();
                    double spendMs = spendNanoTime / 1000000.0;
                    SlowCommandMonitor.slowCommand(command, spendMs, slowCommandThresholdMillisTime);
                    if (commandSpendTimeConfig.getSlowCommandMonitorCallback() != null) {
                        try {
                            commandSpendTimeConfig.getSlowCommandMonitorCallback().callback(command, reply,
                                    spendMs, slowCommandThresholdMillisTime);
                        } catch (Exception e) {
                            ErrorLogCollector.collect(AsyncTask.class, "SlowCommandCallback error", e);
                        }
                    }
                }
            }
            if (logger.isDebugEnabled()) {
                logger.debug("AsyncTask replyCompleted, reply = {}, consid = {}", reply.getClass().getSimpleName(), taskQueue.getChannelInfo().getConsid());
            }
            if (!fromCache) {
                if (hotKeyCache != null && command.getRedisCommand() == RedisCommand.GET) {
                    if (reply instanceof BulkReply) {
                        byte[] key = command.getObjects()[1];
                        byte[] value = ((BulkReply) reply).getRaw();
                        hotKeyCache.tryBuildHotKeyCache(key, value);
                    }
                }
            }
            this.reply = reply;
            if (bigKeyHunter != null) {
                try {
                    bigKeyHunter.checkDownstream(command, reply);
                } catch (Exception e) {
                    ErrorLogCollector.collect(AsyncTask.class, e.getMessage(), e);
                }
            }
            this.taskQueue.callback();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            this.reply = new ErrorReply(e.getMessage());
            this.taskQueue.callback();
        }
    }

    public void replyCompleted(Reply reply) {
        replyCompleted(reply, false);
    }

    public Command getCommand() {
        return command;
    }

    public Reply getReply() {
        return reply;
    }
}
