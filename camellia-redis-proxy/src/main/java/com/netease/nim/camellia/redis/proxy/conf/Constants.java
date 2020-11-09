package com.netease.nim.camellia.redis.proxy.conf;

import com.netease.nim.camellia.core.util.SysUtils;
import com.netease.nim.camellia.redis.proxy.command.async.hotkey.LoggingHoyKeyMonitorCallback;
import com.netease.nim.camellia.redis.proxy.command.async.hotkeycache.DummyHotKeyCacheKeyChecker;
import com.netease.nim.camellia.redis.proxy.command.async.hotkeycache.LoggingHotKeyCacheStatsCallback;
import com.netease.nim.camellia.redis.proxy.command.async.spendtime.LoggingSlowCommandCallback;
import com.netease.nim.camellia.redis.proxy.monitor.LoggingMonitorCallback;

/**
 * 一些默认配置项
 * Created by caojiajun on 2020/2/5.
 */
public class Constants {

    public static class Server {
        public static final int severPort = 6379;
        public static final int consolePort = 16379;
        public static final boolean monitorEnable = false;
        public static final boolean commandSpendTimeMonitorEnable = false;
        public static final String monitorCallbackClassName = LoggingMonitorCallback.class.getName();
        public static final long slowCommandThresholdMillisTime = 2000L;
        public static final String slowCommandCallbackClassName = LoggingSlowCommandCallback.class.getName();
        public static final int monitorIntervalSeconds = 60;

        public static final int workThread = SysUtils.getCpuHalfNum();
        public static final int commandDecodeMaxBatchSize = 256;

        public static final int soBacklog = 1024;
        public static final int soSndbuf = 10 * 1024 * 1024;
        public static final int soRcvbuf = 10 * 1024 * 1024;
        public static final int writeBufferWaterMarkLow = 128 * 1024;
        public static final int writeBufferWaterMarkHigh = 512 * 1024;

        public static final boolean hotKeyMonitorEnable = false;
        public static final long hotKeyMonitorCheckMillis = 1000L;
        public static final long hotKeyMonitorCheckCacheMaxCapacity = 10000L;
        public static final long hotKeyMonitorCheckThreshold = 100;
        public static final int hotKeyMonitorMaxHotKeyCount = 32;
        public static final String hotKeyMonitorCallbackClassName = LoggingHoyKeyMonitorCallback.class.getName();

        public static final boolean hotKeyCacheEnable = false;
        public static final long hotKeyCacheExpireMillis = 10000;
        public static final long hotKeyCacheMaxCapacity = 1000;
        public static final long hotKeyCacheCounterCheckMillis = 1000;
        public static final long hotKeyCacheCounterMaxCapacity = 1000;
        public static final long hotKeyCacheCounterCheckThreshold = 100;
        public static final boolean hotKeyCacheNeedCacheNull = true;
        public static final String hotKeyCacheKeyCheckerClassName = DummyHotKeyCacheKeyChecker.class.getName();
        public static final long hotKeyCacheStatsCallbackIntervalSeconds = 60;
        public static final String hotKeyCacheStatsCallbackClassName = LoggingHotKeyCacheStatsCallback.class.getName();
    }

    public static class Transpond {
        public static final int redisClusterMaxAttempts = 5;
        public static final int heartbeatIntervalSeconds = 60;//若小于等于0则不发心跳
        public static final long heartbeatTimeoutMillis = 10000L;
        public static final int connectTimeoutMillis = 500;
        public static final int failCountThreshold = 10;
        public static final long failBanMillis = 5000L;
        public static final int commandPipelineFlushThreshold = 256;
        public static final QueueType queueType = QueueType.None;
        public static final int defaultTranspondWorkThread = SysUtils.getCpuHalfNum();//if queueType is None, then effective
        public static final MultiWriteMode multiWriteMode = MultiWriteMode.FIRST_RESOURCE_ONLY;
    }

    public static class Remote {
        public static final boolean dynamic = true;
        public static final boolean monitorEnable = true;
        public static final long checkIntervalMillis = 5000;
        public static final int connectTimeoutMillis = 10000;
        public static final int readTimeoutMillis = 60000;
    }
}
