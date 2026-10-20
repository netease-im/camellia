package com.netease.nim.camellia.hot.key.server.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 *
 * Created by caojiajun on 2020/11/5
 */
public class TimeCache implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(TimeCache.class);

    public static volatile long currentMillis = System.currentTimeMillis();
    private static volatile boolean manualModeForTest;

    static {
        Thread thread = new Thread(new TimeCache(), "camellia-time-cache");
        thread.setDaemon(true);
        thread.start();
    }

    @Override
    public void run() {
        while (true) {
            if (!manualModeForTest) {
                currentMillis = System.currentTimeMillis();
            }
            sleep();
        }
    }

    private void sleep() {
            try {
                TimeUnit.MILLISECONDS.sleep(5);
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }
    }

    /** Enables the deterministic clock used by hot-key unit tests. */
    public static void enableManualModeForTest() {
        manualModeForTest = true;
    }

    /** Restores the normal wall-clock updater after a test. */
    public static void disableManualModeForTest() {
        manualModeForTest = false;
        currentMillis = System.currentTimeMillis();
    }
}
