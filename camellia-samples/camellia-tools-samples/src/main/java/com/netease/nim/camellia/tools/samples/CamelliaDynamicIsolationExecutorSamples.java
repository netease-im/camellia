package com.netease.nim.camellia.tools.samples;

import com.netease.nim.camellia.tools.executor.CamelliaDynamicIsolationExecutor;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by caojiajun on 2023/1/5
 */
public class CamelliaDynamicIsolationExecutorSamples {

    public static void main(String[] args) {
        CamelliaDynamicIsolationExecutor executor = new CamelliaDynamicIsolationExecutor("test", 10);
        new Thread(() -> {
            AtomicLong id = new AtomicLong();
            for (int i=0; i<1000; i++) {
                final long start = System.currentTimeMillis();
                executor.submit("1", () -> {
                    long l = id.incrementAndGet();
                    System.out.println("key=1, start, latency = " + (System.currentTimeMillis() - start) + ", id = " + l
                            + ", thread=" + Thread.currentThread().getName() + ",type=" + CamelliaDynamicIsolationExecutor.getCurrentExecutorType());
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        new Thread(() -> {
            AtomicLong id = new AtomicLong();
            for (int i=0; i<1000; i++) {
                final long start = System.currentTimeMillis();
                executor.submit("2", () -> {
                    long l = id.incrementAndGet();
                    System.out.println("key=2, start, latency = " + (System.currentTimeMillis() - start) + ", id = " + l
                            + ", thread=" + Thread.currentThread().getName() + ",type=" + CamelliaDynamicIsolationExecutor.getCurrentExecutorType());
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        new Thread(() -> {
            AtomicLong id = new AtomicLong();
            for (int i=0; i<1000; i++) {
                final long start = System.currentTimeMillis();
                executor.submit("3", () -> {
                    long l = id.incrementAndGet();
                    System.out.println("key=3, start, latency = " + (System.currentTimeMillis() - start) + ", id = " + l
                            + ", thread=" + Thread.currentThread().getName() + ",type=" + CamelliaDynamicIsolationExecutor.getCurrentExecutorType());
                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }
}
