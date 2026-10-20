package com.netease.nim.camellia.tools.executor;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by caojiajun on 2026/10/9
 */
public class CamelliaMergingExecutorTest {

    @Test(expected = IllegalArgumentException.class)
    public void testQueueCountShouldNotLessThanWorkerCount() {
        new CamelliaMergingExecutor<>("test", 1, 2, 10, 1000,
                (oldValue, newValue) -> newValue, tasks -> { });
    }

    @Test
    public void testSameKeyMergedInOneWindow() throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        List<List<String>> batches = Collections.synchronizedList(new ArrayList<>());
        CamelliaMergingExecutorConfig<String> config = new CamelliaMergingExecutorConfig<>("merging",
                (oldValue, newValue) -> Math.max(Integer.parseInt(oldValue), Integer.parseInt(newValue)) + "",
                tasks -> {
                    batches.add(new ArrayList<>(tasks));
                    done.countDown();
                });
        config.setQueueCount(1);
        config.setWorkerCount(1);
        config.setMaxBatchSize(1000);
        config.setIdleWaitMillis(1);
        config.setWindowMillis(200);
        CamelliaMergingExecutor<String> executor = new CamelliaMergingExecutor<>(config);
        try {
            //同一个 key 在一个窗口内提交 1000 次，且 msgid 单调递增（模拟"先小后大"的已读位点上报）
            for (int i = 1; i <= 1000; i++) {
                Assert.assertTrue(executor.submit("same-key", "" + i));
            }
            Assert.assertTrue(executor.submit(new FixedHashKey(7), "999999"));
            Assert.assertTrue(done.await(3, TimeUnit.SECONDS));
            List<String> all = new ArrayList<>();
            for (List<String> batch : batches) {
                all.addAll(batch);
            }
            int sameKeyCount = 0;
            int otherKeyCount = 0;
            for (String value : all) {
                if ("999999".equals(value)) {
                    otherKeyCount++;
                } else {
                    sameKeyCount++;
                }
            }
            //同一个 key 在一个窗口内被合并，而不是每次提交都产生一条
            Assert.assertTrue("sameKeyCount=" + sameKeyCount, sameKeyCount < 1000);
            Assert.assertEquals(1, otherKeyCount);
            //合并规则取最大值
            Assert.assertTrue("expected merged value 1000 in " + all, all.contains("1000"));
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void testMergeRatioBetterThanStreamingBatch() throws Exception {
        int submitCount = 5000;
        //流式批量：每个任务独立处理
        CountDownLatch streamingDone = new CountDownLatch(submitCount);
        AtomicInteger streamingHandled = new AtomicInteger(0);
        CamelliaHashedBatchExecutorConfig<Integer> streamingConfig = new CamelliaHashedBatchExecutorConfig<>("streaming",
                tasks -> {
                    streamingHandled.addAndGet(tasks.size());
                    for (int i = 0; i < tasks.size(); i++) {
                        streamingDone.countDown();
                    }
                });
        streamingConfig.setQueueCount(1);
        streamingConfig.setWorkerCount(1);
        streamingConfig.setMaxBatchSize(64);
        streamingConfig.setIdleWaitMillis(1);
        CamelliaHashedBatchExecutor<Integer> streaming = new CamelliaHashedBatchExecutor<>(streamingConfig);
        try {
            for (int i = 0; i < submitCount; i++) {
                Assert.assertTrue(streaming.submit("same-key", i));
            }
            Assert.assertTrue(streamingDone.await(10, TimeUnit.SECONDS));
        } finally {
            streaming.shutdown();
        }
        Assert.assertEquals(submitCount, streamingHandled.get());

        //窗口合并：同一个 key 在窗口内只保留一条
        CountDownLatch mergedDone = new CountDownLatch(1);
        AtomicInteger mergedHandled = new AtomicInteger(0);
        CamelliaMergingExecutorConfig<Integer> mergedConfig = new CamelliaMergingExecutorConfig<>("merged",
                Math::max,
                tasks -> {
                    mergedHandled.addAndGet(tasks.size());
                    mergedDone.countDown();
                });
        mergedConfig.setQueueCount(1);
        mergedConfig.setWorkerCount(1);
        mergedConfig.setMaxBatchSize(5000);
        mergedConfig.setIdleWaitMillis(1);
        mergedConfig.setWindowMillis(500);
        CamelliaMergingExecutor<Integer> merged = new CamelliaMergingExecutor<>(mergedConfig);
        try {
            for (int i = 0; i < submitCount; i++) {
                Assert.assertTrue(merged.submit("same-key", i));
            }
            Assert.assertTrue(mergedDone.await(10, TimeUnit.SECONDS));
        } finally {
            merged.shutdown();
        }
        Assert.assertTrue("mergedHandled=" + mergedHandled.get(), mergedHandled.get() < submitCount / 10);
    }

    @Test
    public void testSlotLimitAndBackpressure() throws Exception {
        int slotLimit = 10;
        CountDownLatch firstBatchEntered = new CountDownLatch(1);
        CountDownLatch allowFinish = new CountDownLatch(1);
        CamelliaMergingExecutorConfig<Integer> config = new CamelliaMergingExecutorConfig<>("limit",
                Math::max,
                tasks -> {
                    firstBatchEntered.countDown();
                    allowFinish.await(5, TimeUnit.SECONDS);
                });
        config.setQueueCount(1);
        config.setWorkerCount(1);
        config.setMaxBatchSize(1000);
        config.setIdleWaitMillis(1);
        //窗口设得足够长，保证只有"到达槽位上限"才会触发刷新
        config.setWindowMillis(60000);
        config.setMaxAccumulateSize(slotLimit);
        //用 AbortPolicy 让溢出行为可断言（默认的 CallerRunsPolicy 会在调用方同步执行、阻塞在队列令牌上）
        config.setRejectedExecutionHandler(CamelliaMergingExecutor.AbortPolicy::new);
        CamelliaMergingExecutor<Integer> executor = new CamelliaMergingExecutor<>(config);
        try {
            //第一批：10 个不同 key，正好到达上限，触发立即刷新，工作线程在 handler 里阻塞住
            for (int i = 0; i < slotLimit; i++) {
                Assert.assertTrue(executor.submit("first-key-" + i, i));
            }
            Assert.assertTrue(firstBatchEntered.await(3, TimeUnit.SECONDS));

            //工作线程已阻塞且不再消费，此时继续堆积到上限
            for (int i = 0; i < slotLimit; i++) {
                Assert.assertTrue(executor.submit("second-key-" + i, i));
            }
            Assert.assertEquals(slotLimit, executor.getQueueSize(0));
            Assert.assertEquals(slotLimit, executor.getQueueSize());

            //新增 hashKey 会溢出：走拒绝策略做背压，而不是无限增长内存
            try {
                executor.submit("overflow-key", 1);
                Assert.fail("expected RejectedExecutionException");
            } catch (RejectedExecutionException expected) {
                //expected
            }
            //内存上界保持不变
            Assert.assertEquals(slotLimit, executor.getQueueSize(0));

            //同一个 key 的重复提交只是合并、不新增槽位，任何情况下都不应该被拒绝
            Assert.assertTrue(executor.submit("second-key-0", 999));

            allowFinish.countDown();
        } finally {
            allowFinish.countDown();
            executor.shutdown();
        }
    }

    @Test
    public void testDefaultBackpressureDoesNotLoseTask() throws Exception {
        int total = 3000;
        AtomicInteger handled = new AtomicInteger();
        Set<Integer> handledSet = Collections.synchronizedSet(new HashSet<>());
        CamelliaMergingExecutorConfig<Integer> config = new CamelliaMergingExecutorConfig<>("default-backpressure",
                Math::max,
                tasks -> {
                    for (Integer task : tasks) {
                        handled.incrementAndGet();
                        handledSet.add(task);
                    }
                });
        config.setQueueCount(1);
        config.setWorkerCount(1);
        config.setMaxBatchSize(4);
        config.setIdleWaitMillis(1);
        config.setWindowMillis(20);
        //阈值故意配得远小于并发量，强制高频触发"提前刷新 + 溢出"，验证默认背压路径不丢数据
        config.setMaxAccumulateSize(8);
        CamelliaMergingExecutor<Integer> executor = new CamelliaMergingExecutor<>(config);
        try {
            for (int i = 0; i < total; i++) {
                executor.submit("key-" + i, i);
            }
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            while (handled.get() < total && System.nanoTime() < deadline) {
                Thread.sleep(10);
            }
            Assert.assertEquals("handled=" + handled.get(), total, handled.get());
            Assert.assertEquals(total, handledSet.size());
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void testFlushRemainingOnShutdown() throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        List<Integer> handled = Collections.synchronizedList(new ArrayList<>());
        CamelliaMergingExecutorConfig<Integer> config = new CamelliaMergingExecutorConfig<>("shutdown",
                Math::max,
                tasks -> {
                    handled.addAll(tasks);
                    done.countDown();
                });
        config.setQueueCount(4);
        config.setWorkerCount(2);
        config.setMaxBatchSize(100);
        config.setIdleWaitMillis(1);
        //窗口很长，正常情况下不会自然到期，只有停机时的最终刷新才会把它交出去
        config.setWindowMillis(60000);
        CamelliaMergingExecutor<Integer> executor = new CamelliaMergingExecutor<>(config);
        Assert.assertTrue(executor.submit("key1", 1));
        Assert.assertTrue(executor.submit("key1", 5));
        Assert.assertTrue(executor.submit("key1", 3));
        Assert.assertTrue(executor.submit(new FixedHashKey(1), 7));
        //窗口未到期，不应该被处理
        Thread.sleep(100);
        Assert.assertEquals(0, handled.size());
        executor.shutdown();
        Assert.assertTrue("handled=" + handled, done.await(3, TimeUnit.SECONDS));
        Assert.assertEquals(2, handled.size());
        Assert.assertTrue(handled.contains(5));
        Assert.assertTrue(handled.contains(7));
    }

    @Test
    public void testFlushRemainingOnShutdownMoreThanMaxBatchSize() throws Exception {
        int total = 10;
        Set<Integer> handled = Collections.synchronizedSet(new HashSet<>());
        CamelliaMergingExecutorConfig<Integer> config = new CamelliaMergingExecutorConfig<>("shutdown-multi-batch",
                Math::max,
                tasks -> handled.addAll(tasks));
        config.setQueueCount(1);
        config.setWorkerCount(1);
        config.setMaxBatchSize(2);
        config.setIdleWaitMillis(1);
        //窗口很长，只有停机时的最终刷新会处理这些槽位
        config.setWindowMillis(60000);
        CamelliaMergingExecutor<Integer> executor = new CamelliaMergingExecutor<>(config);
        try {
            for (int i = 0; i < total; i++) {
                Assert.assertTrue(executor.submit("key-" + i, i));
            }
            //单队列槽位数超过 maxBatchSize，停机刷新必须循环取到队列为空
            Assert.assertEquals(total, executor.getQueueSize());
            executor.shutdown();
            Assert.assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
            Assert.assertEquals("handled=" + handled, total, handled.size());
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void testSubmitConcurrentWithWindowFlushDoesNotStrandTask() throws Exception {
        int threadCount = 4;
        int perThread = 2000;
        int total = threadCount * perThread;
        Set<Integer> handled = Collections.synchronizedSet(new HashSet<>());
        CamelliaMergingExecutorConfig<Integer> config = new CamelliaMergingExecutorConfig<>("window-race",
                Math::max,
                tasks -> handled.addAll(tasks));
        config.setQueueCount(1);
        config.setWorkerCount(1);
        config.setMaxBatchSize(64);
        config.setIdleWaitMillis(1);
        //窗口极短，让"提交"和"窗口到期后的关闭"持续并发发生
        config.setWindowMillis(1);
        CamelliaMergingExecutor<Integer> executor = new CamelliaMergingExecutor<>(config);
        CountDownLatch start = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();
        List<Thread> threads = new ArrayList<>();
        try {
            for (int t = 0; t < threadCount; t++) {
                final int threadIndex = t;
                Thread thread = new Thread(() -> {
                    try {
                        start.await();
                        for (int i = 0; i < perThread; i++) {
                            int value = threadIndex * perThread + i;
                            executor.submit("key-" + value, value);
                        }
                    } catch (Throwable e) {
                        error.set(e);
                    }
                });
                threads.add(thread);
                thread.start();
            }
            start.countDown();
            for (Thread thread : threads) {
                thread.join(TimeUnit.SECONDS.toMillis(10));
                Assert.assertFalse(thread.isAlive());
            }
            Assert.assertNull(error.get());
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            while (handled.size() < total && System.nanoTime() < deadline) {
                Thread.sleep(5);
            }
            //不能有槽位滞留在 nextFlushTime = 0 的队列里
            Assert.assertEquals("handled=" + handled.size(), total, handled.size());
            Assert.assertEquals(0, executor.getQueueSize());
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
