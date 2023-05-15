package com.netease.nim.camellia.redis.proxy.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * 高并发频繁调用{@link System#currentTimeMillis()}会有性能损耗，所以对于时间要求不太严格的场景可以使用
 * Created by caojiajun on 2020/11/5
 */
public class TimeCache implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(TimeCache.class);

    /**
     * 时间，单位是milliseconds，volatile保证可见性
     */
    public static volatile long currentMillis = System.currentTimeMillis();

    static {
        new Thread(new TimeCache()).start();
    }

    @Override
    public void run() {
        while (true) {
            currentMillis = System.currentTimeMillis();
            try {
                TimeUnit.MILLISECONDS.sleep(5);
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }
}
