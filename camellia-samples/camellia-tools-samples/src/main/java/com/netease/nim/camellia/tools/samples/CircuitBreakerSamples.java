package com.netease.nim.camellia.tools.samples;

import com.netease.nim.camellia.tools.circuitbreaker.CamelliaCircuitBreaker;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by caojiajun on 2022/3/31
 */
public class CircuitBreakerSamples {
    public static void main(String[] args) {
        CamelliaCircuitBreaker circuitBreaker = new CamelliaCircuitBreaker();

        AtomicLong success = new AtomicLong();
        AtomicLong fail = new AtomicLong();
        long start = System.currentTimeMillis();
        new Thread(() -> {
            while (true) {
                if (System.currentTimeMillis() - start <= 10000*2) {
                    boolean allowRequest = circuitBreaker.allowRequest();
                    if (!allowRequest) {
                        System.out.println("quick fail of fail");
                        fail.incrementAndGet();
                        try {
                            TimeUnit.MILLISECONDS.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        continue;
                    }
                    long ret = fail.incrementAndGet();
                    if (ret % 100 == 1) {
                        System.out.println("fail=" + ret);
                    }
                    circuitBreaker.incrementFail();
                    try {
                        TimeUnit.MILLISECONDS.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    break;
                }
            }
            System.out.println("end fail");
        }).start();

        new Thread(() -> {
            while (true) {
                boolean allowRequest = circuitBreaker.allowRequest();
                if (!allowRequest) {
                    System.out.println("quick fail of success");
                    fail.incrementAndGet();
                    try {
                        TimeUnit.MILLISECONDS.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    continue;
                }
                long ret = success.incrementAndGet();
                if (ret % 100 == 1) {
                    System.out.println("success=" + ret);
                }
                circuitBreaker.incrementSuccess();
                try {
                    TimeUnit.MILLISECONDS.sleep(5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
