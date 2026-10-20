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
    private static volatile long lastPublishedMillis = currentMillis;
    private static volatile boolean externalTimeOverride;

    static {
        Thread thread = new Thread(new TimeCache(), "camellia-time-cache");
        thread.setDaemon(true);
        thread.start();
    }

    @Override
    public void run() {
        while (true) {
            long now = System.currentTimeMillis();
            long current = currentMillis;
            // Tests and embedders may inject a clock value through currentMillis. Do not
            // immediately overwrite that value with wall-clock time.
            if (!externalTimeOverride && Math.abs(current - lastPublishedMillis) > 1000) {
                externalTimeOverride = true;
            }
            if (externalTimeOverride) {
                // Resume normal updates once the injected value has been restored to
                // the current wall-clock range.
                if (Math.abs(current - now) <= 1000) {
                    externalTimeOverride = false;
                } else {
                    sleep();
                    continue;
                }
            }
            currentMillis = now;
            lastPublishedMillis = now;
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
}
