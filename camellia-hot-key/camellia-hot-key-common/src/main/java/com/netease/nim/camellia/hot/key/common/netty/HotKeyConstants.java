package com.netease.nim.camellia.hot.key.common.netty;

import com.netease.nim.camellia.tools.utils.SysUtils;

/**
 * Created by caojiajun on 2023/5/8
 */
public class HotKeyConstants {

    public static class Client {
        public static int workThread = SysUtils.getCpuHalfNum();
        public static int bizWorkThread = SysUtils.getCpuHalfNum();
        public static boolean TCP_NODELAY = true;
        public static boolean SO_KEEPALIVE = true;
        public static int SO_RCVBUF = 64 * 1024;
        public static int CLIENT_CONNECT_TIMEOUT_MILLIS = 2000;
        public static int sessionCapacity = 10000;
        public static long getConfigTimeoutMillis = 10*1000L;
        public static long heartbeatIntervalSeconds = 60;
        public static long heartbeatTimeoutMillis = 10*1000L;
        public static long pushIntervalMillis = 100;
        public static int pushBatch = 1000;

        public static int hotKeyConfigReloadIntervalSeconds = 60;
    }

}
