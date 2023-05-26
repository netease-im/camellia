package com.netease.nim.camellia.hot.key.common.netty;

import com.netease.nim.camellia.tools.utils.SysUtils;

/**
 * Created by caojiajun on 2023/5/8
 */
public class HotKeyConstants {

    public static class Client {
        public static int nettyWorkThread = SysUtils.getCpuHalfNum();
        public static int bizWorkThread = SysUtils.getCpuHalfNum();
        public static int bizWorkQueueCapacity = 100000;

        public static boolean TCP_NODELAY = true;
        public static boolean SO_KEEPALIVE = true;
        public static int SO_RCVBUF = 64 * 1024;
        public static int CLIENT_CONNECT_TIMEOUT_MILLIS = 2000;
        public static int sessionCapacity = 10000;
        public static long getConfigTimeoutMillis = 10*1000L;
        public static long pushCacheHitStatsTimeoutMillis = 10*1000L;
        public static long heartbeatIntervalSeconds = 60;
        public static long reloadIntervalSeconds = 120;
        public static long heartbeatTimeoutMillis = 10*1000L;
        public static long pushIntervalMillis = 100;
        public static int pushBatch = 5000;

        public static int capacity = 100000;
        public static int connectNum = 3;

        public static int hotKeyConfigReloadIntervalSeconds = 60;

        public static String source;//标识
    }

    public static class Server {
        public static final int severPort = 7070;
        public static final int consolePort = 17070;

        public static final int nettyBossThread = 1;
        public static final int nettyWorkThread = SysUtils.getCpuNum();
        public static final int bizWorkThread = SysUtils.getCpuNum();
        public static int bizWorkQueueCapacity = 100000;

        public static final boolean tcpNoDelay = true;
        public static final int soBacklog = 1024;
        public static final int soSndbuf = 10 * 1024 * 1024;
        public static final int soRcvbuf = 10 * 1024 * 1024;
        public static final boolean soKeepalive = true;
        public static final int writeBufferWaterMarkLow = 128 * 1024;
        public static final int writeBufferWaterMarkHigh = 512 * 1024;

        public static final int maxNamespace = 1000;
        public static final int hotKeyCacheCounterCapacity = 100000;
        public static final int hotKeyCacheCapacity = 10000;

        public static final int callbackExecutorSize = SysUtils.getCpuHalfNum();
        public static final int hotKeyCallbackIntervalSeconds = 10;

        public static final int topnCount = 100;
        public static final int topnCacheCounterCapacity = 100000;
        public static final int topnCollectSeconds = 60;
        public static final int topnTinyCollectSeconds = 5;
        public static final String topnRedisKeyPrefix = "camellia";
        public static final int topnRedisExpireSeconds = 60*60;

        public static final int monitorIntervalSeconds = 60;
        public static final int monitorHotKeyMaxCount = 20;

        public static int maxHotKeySourceSetSize = 1000;
    }

}
