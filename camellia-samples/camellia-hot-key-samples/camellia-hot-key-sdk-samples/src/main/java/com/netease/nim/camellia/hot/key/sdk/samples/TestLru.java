package com.netease.nim.camellia.hot.key.sdk.samples;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.cache.CacheBuilder;
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

/**
 * Created by caojiajun on 2023/5/19
 */
public class TestLru {

    //高并发下，ConcurrentLinkedHashMap不能及时回收，导致fgc
    public static void main(String[] args) {
        int thread = 2;
        int capacity = 1000;
        test1(thread, capacity);
        test2(thread, capacity);
        test3(thread, capacity);
    }

    private static void test1(int thread, int capacity) {
        ConcurrentLinkedHashMap<String, String> map = new ConcurrentLinkedHashMap.Builder<String, String>()
            .initialCapacity(capacity)
            .maximumWeightedCapacity(capacity)
            .build();

        LongAdder adder = new LongAdder();
        for (int i=0; i<thread; i++) {
            new Thread(() -> {
                long j=0;
                while (true) {
//                    synchronized (map) {
                        map.put(Thread.currentThread().getName() + j, String.valueOf(j));
//                    }
                    j ++;
                    adder.increment();
                }
            }).start();
        }

        Executors.newSingleThreadScheduledExecutor()
                .scheduleAtFixedRate(() -> System.out.println("ConcurrentLinkedHashMap=" + map.size() + ",count=" + adder.sum()), 1, 1, TimeUnit.SECONDS);
    }

    private static void test2(int thread, int capacity) {
        Cache<String, String> map = Caffeine.newBuilder()
                .initialCapacity(capacity).maximumSize(capacity)
                .build();

        LongAdder adder = new LongAdder();
        for (int i=0; i<thread; i++) {
            new Thread(() -> {
                long j=0;
                while (true) {
                    map.put(Thread.currentThread().getName() + j, String.valueOf(j));
                    j ++;
                    adder.increment();
                }
            }).start();
        }

        Executors.newSingleThreadScheduledExecutor()
                .scheduleAtFixedRate(() -> System.out.println("Caffeine=" + map.asMap().size() + ",count=" + adder.sum()), 1, 1, TimeUnit.SECONDS);
    }

    private static void test3(int thread, int capacity) {
        com.google.common.cache.Cache<String,String> map = CacheBuilder.newBuilder()
                .initialCapacity(capacity)
                .maximumSize(capacity)
                .build();

        LongAdder adder = new LongAdder();
        for (int i=0; i<thread; i++) {
            new Thread(() -> {
                long j=0;
                while (true) {
                    map.put(Thread.currentThread().getName() + j, String.valueOf(j));
                    j ++;
                    adder.increment();
                }
            }).start();
        }

        Executors.newSingleThreadScheduledExecutor()
                .scheduleAtFixedRate(() -> System.out.println("guava=" + map.size() + ",count=" + adder.sum()), 1, 1, TimeUnit.SECONDS);
    }
}
