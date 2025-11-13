package com.netease.nim.camellia.console.controller;

/**
 * Create with IntelliJ IDEA
 *
 * @author ChenHongliang
 */
public class HealthStatus {
    private static class TimeCachedThread extends Thread {
        private static volatile long currentTimeMillis = System.currentTimeMillis();

        TimeCachedThread() {
            setName("HealthStatus-TimeCachedThread-" + threadId());
            setDaemon(true);
        }

        static {
            new TimeCachedThread().start();
        }
        public void run() {
            for (;;) {
                currentTimeMillis = System.currentTimeMillis();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignore) {
                }
            }
        }
    }

    static final Integer ONLINE = 0;
    static final Integer OFFLINE = 1;

    static int status = ONLINE;
    static long lastRequestTimestamp = 0;

    /**
     * 更新最近一次请求的时间
     */
    public static void updateRequestTimestamp() {
        lastRequestTimestamp = TimeCachedThread.currentTimeMillis;
    }
}
