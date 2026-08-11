package com.netease.nim.camellia.tools.executor;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by caojiajun on 2026/8/10
 */
public class CamelliaHashedBatchExecutorTest {

    @Test(expected = IllegalArgumentException.class)
    public void testWorkerCountShouldNotGreaterThanQueueCount() {
        new CamelliaHashedBatchExecutor<>("test", 1, 2, 10, tasks -> { });
    }

    @Test
    public void testSameQueueSerialAndDifferentWorkerCanContinue() throws Exception {
        CountDownLatch firstBatchEnter = new CountDownLatch(1);
        CountDownLatch allowFirstBatchFinish = new CountDownLatch(1);
        CountDownLatch allDone = new CountDownLatch(3);
        AtomicInteger concurrent = new AtomicInteger(0);
        AtomicInteger maxConcurrent = new AtomicInteger(0);
        Set<String> threadNames = Collections.synchronizedSet(new HashSet<>());
        List<Integer> result = Collections.synchronizedList(new ArrayList<>());

        CamelliaHashedBatchExecutorConfig<Integer> config = new CamelliaHashedBatchExecutorConfig<>("serial", tasks -> {
            int current = concurrent.incrementAndGet();
            maxConcurrent.updateAndGet(value -> Math.max(value, current));
            threadNames.add(Thread.currentThread().getName());
            try {
                if (tasks.contains(1)) {
                    firstBatchEnter.countDown();
                    allowFirstBatchFinish.await(3, TimeUnit.SECONDS);
                }
                result.addAll(tasks);
                for (Integer ignored : tasks) {
                    allDone.countDown();
                }
            } finally {
                concurrent.decrementAndGet();
            }
        });
        config.setQueueCount(1);
        config.setWorkerCount(1);
        config.setMaxBatchSize(1);
        config.setMinBatchSize(1);
        config.setIdleWaitMillis(1);
        CamelliaHashedBatchExecutor<Integer> executor = new CamelliaHashedBatchExecutor<>(config);
        try {
            Assert.assertTrue(executor.submit("same", 1));
            Assert.assertTrue(firstBatchEnter.await(3, TimeUnit.SECONDS));
            Assert.assertTrue(executor.submit("same", 2));
            Assert.assertTrue(executor.submit("same", 3));
            Thread.sleep(100);
            Assert.assertEquals(0, result.size());
            allowFirstBatchFinish.countDown();
            Assert.assertTrue(allDone.await(3, TimeUnit.SECONDS));
            Assert.assertEquals(1, maxConcurrent.get());
            Assert.assertEquals(3, result.size());
            Assert.assertEquals(Integer.valueOf(1), result.get(0));
            Assert.assertEquals(Integer.valueOf(2), result.get(1));
            Assert.assertEquals(Integer.valueOf(3), result.get(2));
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void testFairDrainFromColdQueue() throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        List<List<Integer>> batches = Collections.synchronizedList(new ArrayList<>());
        CamelliaHashedBatchExecutorConfig<Integer> config = new CamelliaHashedBatchExecutorConfig<>("fair", tasks -> {
            batches.add(new ArrayList<>(tasks));
            if (tasks.contains(100)) {
                done.countDown();
            }
        });
        config.setQueueCount(2);
        config.setWorkerCount(1);
        config.setMaxBatchSize(10);
        config.setMinBatchSize(1);
        config.setPerQueueMaxDrainSize(2);
        config.setIdleWaitMillis(1);
        CamelliaHashedBatchExecutor<Integer> executor = new CamelliaHashedBatchExecutor<>(config);
        try {
            for (int i = 0; i < 20; i++) {
                Assert.assertTrue(executor.submit(new FixedHashKey(0), i));
            }
            Assert.assertTrue(executor.submit(new FixedHashKey(1), 100));
            Assert.assertTrue(done.await(3, TimeUnit.SECONDS));
            boolean coldQueueHandledWithHotQueue = false;
            List<List<Integer>> snapshot;
            synchronized (batches) {
                snapshot = new ArrayList<>(batches);
            }
            for (List<Integer> batch : snapshot) {
                if (batch.contains(100) && batch.size() > 1) {
                    coldQueueHandledWithHotQueue = true;
                    break;
                }
            }
            Assert.assertTrue(coldQueueHandledWithHotQueue);
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void testWaitForMinBatchSizeAtLowWatermark() throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        List<Integer> handled = Collections.synchronizedList(new ArrayList<>());
        CamelliaHashedBatchExecutorConfig<Integer> config = new CamelliaHashedBatchExecutorConfig<>("wait", tasks -> {
            handled.addAll(tasks);
            done.countDown();
        });
        config.setQueueCount(2);
        config.setWorkerCount(1);
        config.setMaxBatchSize(10);
        config.setMinBatchSize(2);
        config.setMaxBatchWaitMillis(300);
        config.setIdleWaitMillis(1);
        CamelliaHashedBatchExecutor<Integer> executor = new CamelliaHashedBatchExecutor<>(config);
        try {
            Assert.assertTrue(executor.submit(new FixedHashKey(0), 1));
            Thread.sleep(50);
            Assert.assertTrue(executor.submit(new FixedHashKey(1), 2));
            Assert.assertTrue(done.await(3, TimeUnit.SECONDS));
            Assert.assertEquals(2, handled.size());
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void testSubmitReturnFalseWhenQueueFull() throws Exception {
        AtomicBoolean rejected = new AtomicBoolean(false);
        CountDownLatch block = new CountDownLatch(1);
        CamelliaHashedBatchExecutorConfig<Integer> config = new CamelliaHashedBatchExecutorConfig<>("reject", tasks -> block.await(3, TimeUnit.SECONDS));
        config.setQueueCount(1);
        config.setWorkerCount(1);
        config.setDynamicQueueCapacity(() -> 1);
        config.setMaxBatchSize(1);
        config.setMinBatchSize(1);
        config.setIdleWaitMillis(1);
        CamelliaHashedBatchExecutor<Integer> executor = new CamelliaHashedBatchExecutor<>(config);
        try {
            Assert.assertTrue(executor.submit("same", 1));
            Thread.sleep(100);
            Assert.assertTrue(executor.submit("same", 2));
            boolean success = executor.submit("same", 3, (hashKey, task, e) -> rejected.set(true));
            Assert.assertFalse(success);
            Assert.assertTrue(rejected.get());
        } finally {
            block.countDown();
            executor.shutdown();
        }
    }

    @Test
    public void testQueueInflightAndOutstandingStats() throws Exception {
        CountDownLatch firstBatchEnter = new CountDownLatch(1);
        CountDownLatch allowFirstBatchFinish = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);
        CamelliaHashedBatchExecutorConfig<Integer> config = new CamelliaHashedBatchExecutorConfig<>("stats", tasks -> {
            firstBatchEnter.countDown();
            allowFirstBatchFinish.await(3, TimeUnit.SECONDS);
            for (Integer ignored : tasks) {
                done.countDown();
            }
        });
        config.setQueueCount(1);
        config.setWorkerCount(1);
        config.setMaxBatchSize(1);
        config.setMinBatchSize(1);
        config.setIdleWaitMillis(1);
        CamelliaHashedBatchExecutor<Integer> executor = new CamelliaHashedBatchExecutor<>(config);
        try {
            Assert.assertTrue(executor.submit("same", 1));
            Assert.assertTrue(firstBatchEnter.await(3, TimeUnit.SECONDS));
            Assert.assertEquals(0, executor.getQueueSize());
            Assert.assertEquals(1, executor.getInflightTaskCount());
            Assert.assertEquals(1, executor.getOutstandingTaskCount());

            Assert.assertTrue(executor.submit("same", 2));
            Assert.assertEquals(1, executor.getQueueSize());
            Assert.assertEquals(1, executor.getInflightTaskCount());
            Assert.assertEquals(2, executor.getOutstandingTaskCount());

            allowFirstBatchFinish.countDown();
            Assert.assertTrue(done.await(3, TimeUnit.SECONDS));
            Assert.assertEquals(0, executor.getQueueSize());
            Assert.assertEquals(0, executor.getInflightTaskCount());
            Assert.assertEquals(0, executor.getOutstandingTaskCount());
        } finally {
            executor.shutdown();
        }
    }

    private static class FixedHashKey {
        private final int hash;

        private FixedHashKey(int hash) {
            this.hash = hash;
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }
}
