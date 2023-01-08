package com.netease.nim.camellia.tools.samples;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.tools.executor.CamelliaDynamicIsolationExecutor;
import com.netease.nim.camellia.tools.executor.CamelliaDynamicIsolationExecutorConfig;
import com.netease.nim.camellia.tools.statistic.CamelliaStatisticsManager;
import com.netease.nim.camellia.tools.statistic.CamelliaStatsData;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by caojiajun on 2023/1/5
 */
public class CamelliaDynamicIsolationExecutorSamples {

    private static final AtomicBoolean stop = new AtomicBoolean(false);
    private static final long statTime = System.currentTimeMillis();
    private static final long maxRunTime = 100*1000L;

    public static void main(String[] args) throws InterruptedException {
        CamelliaStatisticsManager manager = new CamelliaStatisticsManager();
        int thread = 10;
        CamelliaDynamicIsolationExecutorConfig config = new CamelliaDynamicIsolationExecutorConfig("test", () -> thread);
        config.setIsolationThresholdPercentage(() -> 0.3);
        CamelliaDynamicIsolationExecutor executor1 = new CamelliaDynamicIsolationExecutor(config);

        ThreadPoolExecutor executor2 = new ThreadPoolExecutor(thread * 5, thread * 5, 0, TimeUnit.SECONDS, new LinkedBlockingDeque<>());

        //test CamelliaDynamicIsolationExecutor
        test(manager, executor1, null);

        //test CamelliaDynamicIsolationExecutor
        //test(manager, null, executor2);

        Thread.sleep(1000);
        System.exit(-1);
    }

    private static void test(CamelliaStatisticsManager manager, CamelliaDynamicIsolationExecutor executor1, ThreadPoolExecutor executor2) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(10);
        Set<String> isolationKeys = new HashSet<>();
        new Thread(() -> doTask(manager, executor1, executor2, isolationKeys, latch, "fast1", 10000, 50, 50)).start();
        new Thread(() -> doTask(manager, executor1, executor2, isolationKeys, latch, "fast2", 10000, 50, 100)).start();
        new Thread(() -> doTask(manager, executor1, executor2, isolationKeys, latch, "fast3", 10000, 100, 100)).start();
        new Thread(() -> doTask(manager, executor1, executor2, isolationKeys, latch, "fast4", 10000, 100, 100)).start();
        new Thread(() -> doTask(manager, executor1, executor2, isolationKeys, latch, "fast5", 10000, 200, 200)).start();
        new Thread(() -> doTask(manager, executor1, executor2, isolationKeys, latch, "fast6", 10000, 300, 300)).start();
        new Thread(() -> doTask(manager, executor1, executor2, isolationKeys, latch, "fast7", 10000, 400, 400)).start();
        new Thread(() -> doTask(manager, executor1, executor2, isolationKeys, latch, "fast8", 10000, 500, 500)).start();
        new Thread(() -> doTask(manager, executor1, executor2, isolationKeys, latch, "slow1", 1000,2000, 100)).start();
        new Thread(() -> doTask(manager, executor1, executor2, isolationKeys, latch, "slow2", 800,3000, 100)).start();

        latch.await();

        System.out.println("======end,spend=" + (System.currentTimeMillis() - statTime) + "======");
        System.out.println("======total======");
        Map<String, CamelliaStatsData> statsDataAndReset = manager.getStatsDataAndReset();
        for (String isolationKey : isolationKeys) {
            CamelliaStatsData camelliaStatsData = statsDataAndReset.get(isolationKey);
            System.out.println(isolationKey + ",stats=" + JSONObject.toJSON(camelliaStatsData));
            statsDataAndReset.remove(isolationKey);
        }
        System.out.println("======detail======");
        for (String isolationKey : isolationKeys) {
            for (Map.Entry<String, CamelliaStatsData> entry : statsDataAndReset.entrySet()) {
                if (!entry.getKey().startsWith(isolationKey)) continue;
                System.out.println(entry.getKey() + ",stats=" + JSONObject.toJSON(entry.getValue()));
            }
        }
        System.out.println("======type======");
        for (CamelliaDynamicIsolationExecutor.Type type : CamelliaDynamicIsolationExecutor.Type.values()) {
            for (Map.Entry<String, CamelliaStatsData> entry : statsDataAndReset.entrySet()) {
                if (!entry.getKey().equals(type.name())) continue;
                System.out.println(entry.getKey() + ",stats=" + JSONObject.toJSON(entry.getValue()));
            }
        }
    }

    private static void doTask(CamelliaStatisticsManager manager, CamelliaDynamicIsolationExecutor executor1, ThreadPoolExecutor executor2, Set<String> isolationKeys,
                               CountDownLatch latch, String isolationKey, int taskCount, long taskSpendMs, int taskIntervalMs) {
        isolationKeys.add(isolationKey);
        CountDownLatch latch1 = new CountDownLatch(taskCount);
        boolean isBreak = false;
        for (int i=0; i<taskCount; i++) {
            if (isStop()) {
                isBreak = true;
                break;
            }
            final long id = i;
            final long start = System.currentTimeMillis();
            if (executor1 != null) {
                executor1.submit(isolationKey, () -> doTask(id, start, isolationKey, manager, taskSpendMs, latch1));
                sleep(taskIntervalMs);
                continue;
            }
            if (executor2 != null) {
                executor2.submit(() -> doTask(id, start, isolationKey, manager, taskSpendMs, latch1));
                sleep(taskIntervalMs);
            }
        }
        try {
            if (!isBreak) {
                latch1.await();
            }
            latch.countDown();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void doTask(long id, long start, String isolationKey, CamelliaStatisticsManager manager, long taskSpendMs, CountDownLatch latch1) {
        if (isStop()) {
            latch1.countDown();
            return;
        }
        long latency = System.currentTimeMillis() - start;
        CamelliaDynamicIsolationExecutor.Type type = CamelliaDynamicIsolationExecutor.getCurrentExecutorType();
        System.out.println("key=" + isolationKey + ", start, latency = " + latency + ", id = " + id
                + ", thread=" + Thread.currentThread().getName() + ",type=" + type + ",time=" + (System.currentTimeMillis() - statTime));
        manager.update(isolationKey + "|" + type, latency);
        manager.update(isolationKey, latency);
        manager.update(String.valueOf(type), latency);
        sleep(taskSpendMs);
        latch1.countDown();
    }

    private static boolean isStop() {
        if (stop.get()) return true;
        if (System.currentTimeMillis() - statTime > maxRunTime) {
            stop.set(true);
        }
        return stop.get();
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
