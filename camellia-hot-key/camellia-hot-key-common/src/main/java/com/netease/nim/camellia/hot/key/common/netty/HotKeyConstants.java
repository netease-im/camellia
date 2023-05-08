package com.netease.nim.camellia.hot.key.common.netty;

import com.netease.nim.camellia.tools.utils.SysUtils;

/**
 * Created by caojiajun on 2023/5/8
 */
public class HotKeyConstants {

    public static class Client {
        public static int workThread = SysUtils.getCpuNum() * 2;
        public static boolean TCP_NODELAY = true;
        public static boolean SO_KEEPALIVE = true;
        public static int SO_RCVBUF = 64 * 1024;
        public static int CLIENT_CONNECT_TIMEOUT_MILLIS = 2000;
    }

}
