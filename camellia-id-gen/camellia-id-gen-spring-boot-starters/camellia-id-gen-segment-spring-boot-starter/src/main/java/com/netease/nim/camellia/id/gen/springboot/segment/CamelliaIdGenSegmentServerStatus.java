package com.netease.nim.camellia.id.gen.springboot.segment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Created by caojiajun on 2023/1/9
 */
public class CamelliaIdGenSegmentServerStatus {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaIdGenSegmentServerStatus.class);
    private static final Set<Runnable> onlineCallbackSet = new HashSet<>();
    private static final Set<Runnable> offlineCallbackSet = new HashSet<>();

    private static class TimeCachedThread extends Thread {
        private static volatile long currentTimeMillis = System.currentTimeMillis();

        TimeCachedThread() {
            setName("HealthStatus-TimeCachedThread-" + getId());
            setDaemon(true);
        }

        static {
            new TimeCachedThread().start();
        }
        public void run() {
            while (true) {
                currentTimeMillis = System.currentTimeMillis();
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException ignore) {
                }
            }
        }
    }

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
        CamelliaIdGenSegmentServerStatus.status = status;
        if (status == Status.ONLINE) {
            invokeOnlineCallback();
        } else if (status == Status.OFFLINE) {
            invokeOfflineCallback();
        }
    }

    public static void updateLastUseTime() {
        lastUseTime = TimeCachedThread.currentTimeMillis;
    }

    public static boolean isIdle() {
        return isIdle(20);
    }

    public static boolean isIdle(int idleSeconds) {
        return TimeCachedThread.currentTimeMillis - lastUseTime > idleSeconds*1000L;
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
