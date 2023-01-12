package com.netease.nim.camellia.redis.proxy.conf;

import com.netease.nim.camellia.tools.utils.SysUtils;
import com.netease.nim.camellia.redis.proxy.cluster.DefaultProxyClusterModeProvider;
import com.netease.nim.camellia.redis.proxy.route.DynamicConfProxyRouteConfUpdater;
import com.netease.nim.camellia.redis.proxy.auth.ClientAuthByConfigProvider;
import com.netease.nim.camellia.redis.proxy.monitor.LoggingMonitorCallback;

/**
 * 一些默认配置项
 * Created by caojiajun on 2020/2/5.
 */
public class Constants {

    public static class Server {
        public static final int severPort = 6379;
        public static final int serverPortRandSig = -6379;
        public static final int consolePort = 16379;
        public static final int consolePortRandSig = -16379;
        public static final boolean monitorEnable = false;
        public static final int monitorIntervalSeconds = 60;
        public static final String monitorCallbackClassName = LoggingMonitorCallback.class.getName();
        public static final String clientAuthByConfigProvider = ClientAuthByConfigProvider.class.getName();
        public static final boolean clusterModeEnable = false;
        public static final String clusterModeProviderClassName = DefaultProxyClusterModeProvider.class.getName();

        public static final int workThread = SysUtils.getCpuNum();
        public static final int commandDecodeMaxBatchSize = 256;
        public static final int commandDecodeBufferInitializerSize = 32;

        public static final boolean tcpNoDelay = true;
        public static final int soBacklog = 1024;
        public static final int soSndbuf = 10 * 1024 * 1024;
        public static final int soRcvbuf = 10 * 1024 * 1024;
        public static final boolean soKeepalive = false;
        public static final boolean tcpQuickAck = false;
        public static final int readerIdleTimeSeconds = -1;
        public static final int writerIdleTimeSeconds = -1;
        public static final int allIdleTimeSeconds = -1;
        public static final int writeBufferWaterMarkLow = 128 * 1024;
        public static final int writeBufferWaterMarkHigh = 512 * 1024;

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

    public static class Transpond {
        public static final int redisClusterMaxAttempts = 5;
        public static final int heartbeatIntervalSeconds = 60;//若小于等于0则不发心跳
        public static final long heartbeatTimeoutMillis = 10000L;
        public static final int connectTimeoutMillis = 500;
        public static final int failCountThreshold = 5;
        public static final long failBanMillis = 5000L;
        public static final int defaultTranspondWorkThread = SysUtils.getCpuHalfNum();//if queueType is None, then effective
        public static final MultiWriteMode multiWriteMode = MultiWriteMode.FIRST_RESOURCE_ONLY;
        public static final boolean preheat = true;//预热，若开启，则启动proxy时会预先建立好到后端redis的连接

        public static final boolean closeIdleConnection = true;//是否关闭空闲连接（到后端redis的）
        public static final long checkIdleConnectionThresholdSeconds = 60 * 10;//判断一个连接空闲的阈值，单位秒
        public static final int closeIdleConnectionDelaySeconds = 60;//判断一个连接空闲后，再过多少秒去执行关闭操作

        public static final int soSndbuf = 10 * 1024 * 1024;
        public static final int soRcvbuf = 10 * 1024 * 1024;
        public static final boolean tcpNoDelay = true;
        public static final boolean soKeepalive = true;
        public static final boolean tcpQuickAck = false;
        public static final int writeBufferWaterMarkLow = 128 * 1024;
        public static final int writeBufferWaterMarkHigh = 512 * 1024;
    }

    public static class Remote {
        public static final boolean dynamic = true;
        public static final boolean monitorEnable = true;
        public static final long checkIntervalMillis = 5000;
        public static final int connectTimeoutMillis = 10000;
        public static final int readTimeoutMillis = 60000;
    }

    public static class Custom {
        public static final String proxyRouteConfUpdaterClassName = DynamicConfProxyRouteConfUpdater.class.getName();
        public static final boolean dynamic = true;
        public static final long reloadIntervalMillis = 10 * 60 * 1000;//避免ProxyRouteConfUpdater更新丢失，兜底轮询的间隔
    }
}
