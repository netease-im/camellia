package com.netease.nim.camellia.redis.proxy.command.async;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.command.async.bigkey.BigKeyHunter;
import com.netease.nim.camellia.redis.proxy.command.async.converter.Converters;
import com.netease.nim.camellia.redis.proxy.command.async.hotkeycache.HotKeyCache;
import com.netease.nim.camellia.redis.proxy.command.async.spendtime.CommandSpendTimeConfig;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.monitor.CommandSpendMonitor;
import com.netease.nim.camellia.redis.proxy.monitor.RedisMonitor;
import com.netease.nim.camellia.redis.proxy.monitor.SlowCommandMonitor;
import com.netease.nim.camellia.redis.proxy.netty.ChannelInfo;
import com.netease.nim.camellia.redis.proxy.reply.BulkReply;
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
    private final Converters converters;

    public AsyncTask(AsyncTaskQueue taskQueue, Command command,
                     CommandSpendTimeConfig commandSpendTimeConfig, BigKeyHunter bigKeyHunter, Converters converters) {
        this.command = command;
        this.taskQueue = taskQueue;
        this.taskQueue.setConverters(converters);
        this.commandSpendTimeConfig = commandSpendTimeConfig;
        if (RedisMonitor.isCommandSpendTimeMonitorEnable()) {
            startTime = System.nanoTime();
        }
        this.bigKeyHunter = bigKeyHunter;
        this.converters = converters;
    }

    public void setHotKeyCache(HotKeyCache hotKeyCache) {
        this.hotKeyCache = hotKeyCache;
    }

    public void replyCompleted(Reply reply, boolean fromCache) {
        try {
            if (command != null) {
                if (converters != null && !fromCache) {
                    try {
                        converters.convertReply(command, reply);
                    } catch (Exception e) {
                        ErrorLogCollector.collect(AsyncTask.class, e.getMessage(), e);
                    }
                }
            }
            if (command != null) {
                try {
                    if (startTime > 0) {
                        long spendNanoTime = System.nanoTime() - startTime;
                        ChannelInfo channelInfo = command.getChannelInfo();
                        Long bid = channelInfo == null ? null : channelInfo.getBid();
                        String bgroup = channelInfo == null ? null : channelInfo.getBgroup();
                        CommandSpendMonitor.incrCommandSpendTime(bid, bgroup, command.getName(), spendNanoTime);
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
                } catch (Exception e) {
                    ErrorLogCollector.collect(AsyncTask.class, e.getMessage(), e);
                }
            }
            if (logger.isDebugEnabled()) {
                logger.debug("AsyncTask replyCompleted, command = {}, reply = {}, consid = {}",
                        command == null ? null : command.getName(), reply.getClass().getSimpleName(), taskQueue.getChannelInfo().getConsid());
            }
            if (command != null) {
                try {
                    if (!fromCache) {
                        if (hotKeyCache != null && command.getRedisCommand() == RedisCommand.GET) {
                            if (reply instanceof BulkReply) {
                                byte[] key = command.getObjects()[1];
                                byte[] value = ((BulkReply) reply).getRaw();
                                hotKeyCache.tryBuildHotKeyCache(key, value);
                            }
                        }
                    }
                } catch (Exception e) {
                    ErrorLogCollector.collect(AsyncTask.class, e.getMessage(), e);
                }
            }
            if (command != null) {
                if (bigKeyHunter != null) {
                    try {
                        bigKeyHunter.checkReply(command, reply);
                    } catch (Exception e) {
                        ErrorLogCollector.collect(AsyncTask.class, e.getMessage(), e);
                    }
                }
            }
            this.reply = reply;
            this.taskQueue.callback();
        } catch (Exception e) {
            ErrorLogCollector.collect(AsyncTask.class, e.getMessage(), e);
            this.reply = reply;
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
