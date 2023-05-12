package com.netease.nim.camellia.hot.key.sdk.samples;

import io.netty.util.internal.shaded.org.jctools.queues.MpscArrayQueue;
import io.netty.util.internal.shaded.org.jctools.queues.MpscUnboundedArrayQueue;
import io.netty.util.internal.shaded.org.jctools.queues.atomic.MpscAtomicArrayQueue;
import org.jctools.queues.MpscLinkedQueue;
import org.jctools.queues.atomic.MpscLinkedAtomicQueue;

import java.util.*;
import java.util.concurrent.*;

/**
 * Created by caojiajun on 2023/5/12
 */
public class TestMpscQueue {

    public static void main(String[] args) throws Exception {
        int thread = 5;
        long count = 100*10000L;
        while (true) {
            test(new LinkedBlockingQueue<>(100 * 10000), thread, count);
            test(new ArrayBlockingQueue<>(100 * 10000), thread, count);
            test(new ConcurrentLinkedQueue<>(), thread, count);
            test(new MpscArrayQueue<>(100 * 10000), thread, count);
            test(new MpscLinkedQueue<>(), thread, count);
            test(new MpscUnboundedArrayQueue<>(1000), thread, count);
            test(new MpscAtomicArrayQueue<>(100 * 10000), thread, count);
            test(new MpscLinkedAtomicQueue<>(), thread, count);
            System.out.println("====");
        }
    }

    public static void test(Queue<String> queue, int thread, long count) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch end = new CountDownLatch(1);
        for (int i=0; i<thread; i++) {
            int finalI = i;
            new Thread(() -> {
                try {
                    start.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("produce-start-" + finalI);
                for (int i1 = 0; i1 <count; i1++) {
                    boolean offer = queue.offer(UUID.randomUUID().toString());
                    if (!offer) {
                        System.out.println("ERROR");
                        System.exit(-1);
                    }
                }
                System.out.println("produce-end-" + finalI);
            }).start();
        }
        new Thread(() -> {
            long time = 0;
            long total = 0;
            start.countDown();
            while (true) {
                String poll = queue.poll();
                if (poll != null) {
                    if (total == 0) {
                        System.out.println("consume-start");
                        time = System.currentTimeMillis();
                    }
                    total ++;
                    if (total >= thread * count) {
                        break;
                    }
//                    if (total % 10000 == 0) {
//                        System.out.println("consume=" + total);
//                    }
                }
            }
            System.out.println("consume-end");
            String result = queue.getClass().getName() + ", spend=" + (System.currentTimeMillis() - time);
            System.out.println(result);
            end.countDown();
        }).start();
        end.await();
    }
}
