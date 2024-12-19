package com.netease.nim.camellia.redis.proxy.netty;

import com.netease.nim.camellia.redis.proxy.util.TimeCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 *
 * Created by caojiajun on 2019/11/28.
 */
public class ServerStatus {

    private static final Logger logger = LoggerFactory.getLogger(ServerStatus.class);

    private static Status status = Status.ONLINE;
    private static long lastUseTime = System.currentTimeMillis();
    private static final Set<Runnable> onlineCallbackSet = new HashSet<>();
    private static final Set<Runnable> offlineCallbackSet = new HashSet<>();

    public enum Status {
        ONLINE,
        OFFLINE,
        ;
    }

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(ServerStatus::invokeOfflineCallback));
    }

    public static Status getStatus() {
        return status;
    }

    public static void setStatus(Status status) {
        ServerStatus.status = status;
        if (status == Status.ONLINE) {
            invokeOnlineCallback();
        } else if (status == Status.OFFLINE) {
            invokeOfflineCallback();
        }
    }

    public static void updateLastUseTime() {
        lastUseTime = TimeCache.currentMillis;
    }

    public static boolean isIdle() {
        return TimeCache.currentMillis - lastUseTime > 10*1000L;
    }

    public static void invokeOnlineCallback() {
        for (Runnable runnable : onlineCallbackSet) {
            try {
                runnable.run();
            } catch (Exception e) {
                logger.error("online callback error", e);
            }
        }
    }

    public static void invokeOfflineCallback() {
        for (Runnable runnable : offlineCallbackSet) {
            try {
                runnable.run();
            } catch (Exception e) {
                logger.error("offline callback error", e);
            }
        }
    }

    public static synchronized void registerOnlineCallback(Runnable task) {
        onlineCallbackSet.add(task);
    }

    public static synchronized void registerOfflineCallback(Runnable task) {
        offlineCallbackSet.add(task);
    }
}
