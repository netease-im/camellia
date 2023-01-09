package com.netease.nim.camellia.delayqueue.server.springboot;

import java.util.concurrent.TimeUnit;

/**
 *
 * Created by caojiajun on 2019/11/28.
 */
public class CamelliaDelayQueueServerStatus {

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
        CamelliaDelayQueueServerStatus.status = status;
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
}
