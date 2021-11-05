package com.netease.nim.camellia.redis.proxy.command.async.bigkey;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.util.CamelliaMapUtils;
import com.netease.nim.camellia.redis.proxy.util.ExecutorUtils;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * Created by caojiajun on 2020/11/11
 */
public class LoggingBigKeyMonitorCallback implements BigKeyMonitorCallback {

    private static final Logger logger = LoggerFactory.getLogger("camellia.redis.proxy.bigKeyStats");
    private static ConcurrentHashMap<String, AtomicLong> logMap = new ConcurrentHashMap<>();

    private static final String BUFFER_MAX_SIZE_CONF_KEY = "logging.big.key.monitor.buffer.max.size";
    private static final int defaultBufferMaxSize = 100;

    static {
        ExecutorUtils.scheduleAtFixedRate(LoggingBigKeyMonitorCallback::printLog, 5, 5, TimeUnit.SECONDS);
    }

    private static void printLog() {
        try {
            if (LoggingBigKeyMonitorCallback.logMap.isEmpty()) return;
            ConcurrentHashMap<String, AtomicLong> logMap = LoggingBigKeyMonitorCallback.logMap;
            LoggingBigKeyMonitorCallback.logMap = new ConcurrentHashMap<>();
            for (Map.Entry<String, AtomicLong> entry : logMap.entrySet()) {
                logger.warn(entry.getKey() + ", count = {}", entry.getValue().get());
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public void callbackRequest(Command command, byte[] key, long size, long threshold) {
        try {
            String log = "big key for request, command.context = " + command.getCommandContext() + ", command = " + command.getRedisCommand()
                    + ", key = " + Utils.bytesToString(key) + ", size = " + size + ", threshold = " + threshold;
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

    @Override
    public void callbackReply(Command command, Reply reply, byte[] key, long size, long threshold) {
        try {
            String log = "big key for reply, command.context = " + command.getCommandContext() + ", command = " + command.getRedisCommand()
                    + ", key = " + Utils.bytesToString(key) + ", size = " + size + ", threshold = " + threshold;
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
