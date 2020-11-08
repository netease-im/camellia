package com.netease.nim.camellia.redis.proxy.util;

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

    static {
        new Thread(new TimeCache()).start();
    }

    @Override
    public void run() {
        while (true) {
            currentMillis = System.currentTimeMillis();
            try {
                TimeUnit.MILLISECONDS.sleep(50);
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }
}
