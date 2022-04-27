package com.netease.nim.camellia.tools.samples;


import com.netease.nim.camellia.tools.executor.CamelliaHashedExecutor;

import java.util.concurrent.TimeUnit;

/**
 * Created by caojiajun on 2022/2/17
 */
public class CamelliaHashedExecutorSamples {
    public static void main(String[] args) {
        String name = "sample";
        int poolSize = Runtime.getRuntime().availableProcessors() * 2;
        int queueSize = 100000;
        CamelliaHashedExecutor.RejectedExecutionHandler rejectedExecutionHandler = new CamelliaHashedExecutor.CallerRunsPolicy();
        CamelliaHashedExecutor executor = new CamelliaHashedExecutor(name, poolSize, queueSize, rejectedExecutionHandler);

        //相同hashKey的两个任务确保是单线程顺序执行的

        executor.submit("key1", () -> {
            System.out.println("key1 start1, thread=" + Thread.currentThread().getName());
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("key1 end1, thread=" + Thread.currentThread().getName());
        });

        executor.submit("key2", () -> {
            System.out.println("key2 start1, thread=" + Thread.currentThread().getName());
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("key2 end1, thread=" + Thread.currentThread().getName());
        });

        executor.submit("key1", () -> {
            System.out.println("key1 start2, thread=" + Thread.currentThread().getName());
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("key1 end2, thread=" + Thread.currentThread().getName());
        });

        executor.submit("key2", () -> {
            System.out.println("key2 start2, thread=" + Thread.currentThread().getName());
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("key2 end2, thread=" + Thread.currentThread().getName());
        });
    }
}
