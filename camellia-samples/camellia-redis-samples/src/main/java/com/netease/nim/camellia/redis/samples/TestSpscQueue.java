package com.netease.nim.camellia.redis.samples;

import io.netty.util.internal.shaded.org.jctools.queues.SpscLinkedQueue;

import java.util.ArrayDeque;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by caojiajun on 2023/6/25
 */
public class TestSpscQueue {

    public static void main(String[] args) {
        while (true) {
            int c = 30000000;
            test(new LinkedBlockingQueue<>(1024 * 3), c);
            test(new SpscLinkedQueue<>(), c);
            test(new LinkedList<>(), c);
            test(new ArrayDeque<>(1024), c);
            test(new ConcurrentLinkedQueue<>(), c);
        }
    }

    private static void test(Queue<String> queue, int c) {
        AtomicBoolean start = new AtomicBoolean(true);
        new Thread(() -> {
            while (start.get()) {
                System.out.println("size=" + queue.size());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
        long startTime = System.currentTimeMillis();
        for (int i=0;i < c; i++) {
            queue.offer(String.valueOf(i));
            queue.poll();
        }
        System.out.println(queue.getClass().getName() + ",spend=" + (System.currentTimeMillis() - startTime));
        start.set(false);
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
