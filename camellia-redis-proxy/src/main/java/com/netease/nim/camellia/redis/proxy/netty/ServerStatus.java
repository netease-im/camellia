package com.netease.nim.camellia.redis.proxy.netty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 *
 * Created by caojiajun on 2019/11/28.
 */
public class ServerStatus {

    private static final Logger logger = LoggerFactory.getLogger(ServerStatus.class);

    private static Status status = Status.ONLINE;
    private static long lastUseTime = System.currentTimeMillis();
    private static long currentTime = System.currentTimeMillis();

    static {
        Thread thread = new Thread(() -> {
            while (true) {
                try {
                    TimeUnit.SECONDS.sleep(1);
                    currentTime = System.currentTimeMillis();
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        });
        thread.setName("server-status-time");
        thread.setDaemon(true);
        thread.start();
    }

    public enum Status {
        ONLINE,
        OFFLINE,
        ;
    }

    public static long getCurrentTimeMillis() {
        return currentTime;
    }

    public static Status getStatus() {
        return status;
    }

    public static void setStatus(Status status) {
        ServerStatus.status = status;
    }

    public static void updateLastUseTime() {
        lastUseTime = currentTime;
    }

    public static boolean isIdle() {
        return currentTime - lastUseTime > 10*1000L;
    }
}
