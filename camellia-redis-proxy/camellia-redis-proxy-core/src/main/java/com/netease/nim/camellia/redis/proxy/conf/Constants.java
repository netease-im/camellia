package com.netease.nim.camellia.redis.proxy.conf;

/**
 * 一些默认配置项
 * Created by caojiajun on 2020/2/5.
 */
public class Constants {

    public static class Server {
        public static final int serverPortRandSig = -6379;
        public static final int consolePort = 16379;
        public static final int consolePortRandSig = -16379;
        public static final int commandDecodeMaxBatchSize = 256;
        public static final int commandDecodeBufferInitializerSize = 32;

        public static final int hotKeyMonitorCheckCacheMaxCapacity = 100000;
        public static final long hotKeyMonitorCheckThreshold = 500;
        public static final int hotKeyMonitorMaxHotKeyCount = 32;

        public static final long hotKeyCacheExpireMillis = 10000;
        public static final int hotKeyCacheMaxCapacity = 1000;
        public static final long hotKeyCacheCounterCheckMillis = 1000;
        public static final int hotKeyCacheCounterMaxCapacity = 100000;
        public static final long hotKeyCacheCounterCheckThreshold = 100;
        public static final boolean hotKeyCacheNeedCacheNull = true;
        public static final long hotKeyCacheStatsCallbackIntervalSeconds = 10;

        public static final boolean monitorDataMaskPassword = true;//对外暴露的监控数据是否把密码隐藏（用*代替）
    }

    public static class Upstream {
        public static final int redisClusterMaxAttempts = 5;
        public static final int heartbeatIntervalSeconds = 60;//若小于等于0则不发心跳
        public static final long heartbeatTimeoutMillis = 10000L;
        public static final int connectTimeoutMillis = 1000;
        public static final int failCountThreshold = 5;
        public static final long failBanMillis = 5000L;

        public static final boolean closeIdleConnection = true;//是否关闭空闲连接（到后端redis的）
        public static final long checkIdleConnectionThresholdSeconds = 60 * 10;//判断一个连接空闲的阈值，单位秒
        public static final int closeIdleConnectionDelaySeconds = 60;//判断一个连接空闲后，再过多少秒去执行关闭操作
    }

    public static class Remote {
        public static final boolean dynamic = true;
        public static final boolean monitorEnable = true;
        public static final long checkIntervalMillis = 5000;
        public static final int connectTimeoutMillis = 10000;
        public static final int readTimeoutMillis = 60000;
    }

    public static class Custom {
        public static final boolean dynamic = true;
        public static final long reloadIntervalMillis = 10 * 60 * 1000;//避免ProxyRouteConfUpdater更新丢失，兜底轮询的间隔
    }
}
