package com.netease.nim.camellia.redis.proxy.command.async.spendtime;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.core.util.CamelliaMapUtils;
import com.netease.nim.camellia.redis.proxy.util.ExecutorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * Created by caojiajun on 2020/10/22
 */
public class LoggingSlowCommandMonitorCallback implements SlowCommandMonitorCallback {

    private static final Logger logger = LoggerFactory.getLogger("camellia.redis.proxy.slowCommandStats");

    private static ConcurrentHashMap<String, AtomicLong> logMap = new ConcurrentHashMap<>();

    private static final String BUFFER_MAX_SIZE_CONF_KEY = "logging.slow.command.monitor.buffer.max.size";
    private static final int defaultBufferMaxSize = 100;

    static {
        ExecutorUtils.scheduleAtFixedRate(LoggingSlowCommandMonitorCallback::printLog, 5, 5, TimeUnit.SECONDS);
    }

    private static void printLog() {
        try {
            if (LoggingSlowCommandMonitorCallback.logMap.isEmpty()) return;
            ConcurrentHashMap<String, AtomicLong> logMap = LoggingSlowCommandMonitorCallback.logMap;
            LoggingSlowCommandMonitorCallback.logMap = new ConcurrentHashMap<>();
            for (Map.Entry<String, AtomicLong> entry : logMap.entrySet()) {
                logger.warn(entry.getKey() + ", count = {}", entry.getValue().get());
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public void callback(Command command, Reply reply, double spendMillis, long thresholdMillis) {
        try {
            String log = "slow command, command.context = " + command.getCommandContext() + ", spendMs = " + spendMillis + ", thresholdMs = " + thresholdMillis
                    + ", command = " + command.getRedisCommand() + ", keys = " + command.getKeysStr();
            if (logMap.size() >= ProxyDynamicConf.getInt(BUFFER_MAX_SIZE_CONF_KEY, defaultBufferMaxSize)) {
                logger.warn(log + ", count = 1");
                return;
            }
            AtomicLong count = CamelliaMapUtils.computeIfAbsent(logMap, log, k -> new AtomicLong());
            count.incrementAndGet();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
