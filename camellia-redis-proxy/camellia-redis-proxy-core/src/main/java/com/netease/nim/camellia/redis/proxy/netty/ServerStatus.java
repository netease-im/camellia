package com.netease.nim.camellia.redis.proxy.netty;

import com.netease.nim.camellia.redis.proxy.util.TimeCache;

/**
 *
 * Created by caojiajun on 2019/11/28.
 */
public class ServerStatus {

    private static Status status = Status.ONLINE;
    private static long lastUseTime = System.currentTimeMillis();

    public enum Status {
        ONLINE,
        OFFLINE,
        ;
    }

    public static Status getStatus() {
        return status;
    }

    public static void setStatus(Status status) {
        ServerStatus.status = status;
    }

    public static void updateLastUseTime() {
        lastUseTime = TimeCache.currentMillis;
    }

    public static boolean isIdle() {
        return TimeCache.currentMillis - lastUseTime > 10*1000L;
    }
}
