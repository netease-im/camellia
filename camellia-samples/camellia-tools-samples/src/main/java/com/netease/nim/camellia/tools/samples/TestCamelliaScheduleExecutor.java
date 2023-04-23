package com.netease.nim.camellia.tools.samples;

import com.netease.nim.camellia.tools.executor.CamelliaScheduleExecutor;

import java.util.Date;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by caojiajun on 2023/4/23
 */
public class TestCamelliaScheduleExecutor {

    private static final AtomicInteger id1 = new AtomicInteger();
    private static final AtomicInteger id2 = new AtomicInteger();

    public static void main(String[] args) throws InterruptedException {
        CamelliaScheduleExecutor executor = new CamelliaScheduleExecutor("test", 8);
        CamelliaScheduleExecutor.Task task = executor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                System.out.println("start");
                if (id1.incrementAndGet() % 3 == 2) {
                    System.out.println("error");
                    throw new IllegalArgumentException("error");
                }
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("end");
                System.out.println("invoke=" + new Date() + "|" + Thread.currentThread().getName());
            }
        }, 500, 1000, TimeUnit.MILLISECONDS);
        Thread.sleep(20*1000);
        task.cancel();

        System.out.println("=======================");

        ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(10);
        ScheduledFuture<?> future = executorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                System.out.println("start");
                if (id2.incrementAndGet() % 3 == 2) {
                    System.out.println("error");
                    throw new IllegalArgumentException("error");
                }
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("end");
                System.out.println("invoke=" + new Date() + "|" + Thread.currentThread().getName());
            }
        }, 500, 1000, TimeUnit.MILLISECONDS);

        Thread.sleep(20*1000);
        future.cancel(false);
    }
}
