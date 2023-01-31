package com.netease.nim.camellia.tools.samples;

import com.netease.nim.camellia.tools.executor.CamelliaLinearInitializationExecutor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by caojiajun on 2023/1/31
 */
public class CamelliaLinearInitializationExecutorSamples {

    private static final AtomicLong c = new AtomicLong();

    public static void main(String[] args) {

        CamelliaLinearInitializationExecutor<String, String> executor = new CamelliaLinearInitializationExecutor<>("test", key -> {
            long l = c.incrementAndGet();
            if (l == 1) {
                System.out.println("init fail");
                return null;
            }
            if (l == 2) {
                System.out.println("init error");
                throw new RuntimeException("ex");
            }
            System.out.println("init success");
            return key + "~target";
        });

        AtomicLong result = new AtomicLong();

        CountDownLatch latch = new CountDownLatch(1);
        for (int k=0; k<10; k++) {
            final int finalKey = k;
            new Thread(() -> {
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                for (int i = 0; i < 100; i++) {
                    final String task = finalKey + "_" + i;
                    long time = System.currentTimeMillis();
                    CompletableFuture<String> future = executor.getOrInitialize("abc");
                    future.thenAccept(s -> {
                        String p = "task=" + task + ",index=" + result.incrementAndGet() + ",latency=" + (System.currentTimeMillis() - time) + ",result=" + s + ",thread=" + Thread.currentThread().getName();
                        System.out.println(p);
                    });
                    if (i==10) {
                        try {
                            TimeUnit.MILLISECONDS.sleep(1);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }, "task-" + k).start();
        }
        latch.countDown();
    }
}
