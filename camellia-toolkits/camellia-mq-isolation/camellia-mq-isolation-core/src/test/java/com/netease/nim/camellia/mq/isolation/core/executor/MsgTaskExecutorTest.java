package com.netease.nim.camellia.mq.isolation.core.executor;

import com.netease.nim.camellia.core.client.env.ThreadContextSwitchStrategy;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class MsgTaskExecutorTest {

    @Test
    public void shouldUsePlatformExecutorWhenDisabledAtStartup() throws Exception {
        MsgTaskExecutor executor = new MsgTaskExecutor("platform-test", 1, false);
        try {
            Assert.assertEquals("platform", executor.getMode());
            CountDownLatch completed = new CountDownLatch(1);
            AtomicBoolean platformThread = new AtomicBoolean();
            Assert.assertTrue(executor.execute(() -> {
                platformThread.set(Thread.currentThread().getName().startsWith("camellia-mq-isolation[platform-test]"));
                completed.countDown();
            }));
            Assert.assertTrue(completed.await(5, TimeUnit.SECONDS));
            Assert.assertTrue(platformThread.get());
            Assert.assertFalse(isVirtualThread());
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void shouldUseVirtualThreadWhenEnabledAtStartup() throws Exception {
        Assume.assumeTrue("virtual threads require Java 21+", javaVersion() >= 21);
        MsgTaskExecutor executor = new MsgTaskExecutor("virtual-test", 4, true);
        try {
            Assert.assertEquals("virtual", executor.getMode());
            int taskCount = 8;
            CountDownLatch completed = new CountDownLatch(taskCount);
            AtomicInteger virtualThreads = new AtomicInteger();
            for (int i = 0; i < taskCount; i++) {
                executor.execute(() -> {
                    if (isVirtualThreadQuietly()) {
                        virtualThreads.incrementAndGet();
                    }
                    completed.countDown();
                });
            }
            Assert.assertTrue(completed.await(10, TimeUnit.SECONDS));
            Assert.assertEquals(taskCount, virtualThreads.get());
            Assert.assertEquals(0, executor.getCurrentThreads());
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void shouldKeepStartupModeWhenConfigChanges() throws Exception {
        AtomicBoolean virtualEnabled = new AtomicBoolean(false);
        MsgTaskExecutor executor = new MsgTaskExecutor("fixed-mode-test", 1, virtualEnabled.get());
        try {
            virtualEnabled.set(true);
            Assert.assertEquals("platform", executor.getMode());
            CountDownLatch completed = new CountDownLatch(1);
            AtomicBoolean platformThread = new AtomicBoolean();
            Assert.assertTrue(executor.execute(() -> {
                platformThread.set(Thread.currentThread().getName().startsWith("camellia-mq-isolation[fixed-mode-test]"));
                completed.countDown();
            }));
            Assert.assertTrue(completed.await(5, TimeUnit.SECONDS));
            Assert.assertTrue(platformThread.get());
            Assert.assertFalse(isVirtualThread());
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void shouldRunTaskInlineWhenPlatformPoolIsSaturated() throws Exception {
        MsgTaskExecutor executor = new MsgTaskExecutor("inline-test", 1, false);
        try {
            CountDownLatch blockerStarted = new CountDownLatch(1);
            CountDownLatch releaseBlocker = new CountDownLatch(1);
            Assert.assertTrue(executor.execute(() -> {
                blockerStarted.countDown();
                await(releaseBlocker);
            }));
            Assert.assertTrue(blockerStarted.await(5, TimeUnit.SECONDS));

            CountDownLatch inlineRan = new CountDownLatch(1);
            // The single pool thread is busy, so the fixed-pool backpressure semantics must run this inline.
            Assert.assertTrue(executor.execute(inlineRan::countDown));
            Assert.assertTrue(inlineRan.await(5, TimeUnit.SECONDS));
            releaseBlocker.countDown();
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void shouldBlockSubmittersInsteadOfGrowingUnboundedInVirtualMode() throws Exception {
        Assume.assumeTrue("virtual threads require Java 21+", javaVersion() >= 21);
        MsgTaskExecutor executor = new MsgTaskExecutor("virtual-permit-test", 1, true);
        try {
            CountDownLatch blockerStarted = new CountDownLatch(1);
            CountDownLatch releaseBlocker = new CountDownLatch(1);
            Assert.assertTrue(executor.execute(() -> {
                blockerStarted.countDown();
                await(releaseBlocker);
            }));
            Assert.assertTrue(blockerStarted.await(5, TimeUnit.SECONDS));

            CountDownLatch secondSubmitted = new CountDownLatch(1);
            CountDownLatch secondReturned = new CountDownLatch(1);
            Thread submitter = new Thread(() -> {
                secondSubmitted.countDown();
                executor.execute(secondReturned::countDown);
            }, "virtual-permit-submitter");
            submitter.start();
            Assert.assertTrue(secondSubmitted.await(5, TimeUnit.SECONDS));
            Assert.assertFalse("second submit must wait for the only permit",
                    secondReturned.await(300, TimeUnit.MILLISECONDS));
            releaseBlocker.countDown();
            Assert.assertTrue(secondReturned.await(5, TimeUnit.SECONDS));
            submitter.join(TimeUnit.SECONDS.toMillis(5));
            Assert.assertFalse(submitter.isAlive());
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void shouldRejectTaskWithoutExceptionAfterShutdown() throws Exception {
        MsgTaskExecutor executor = new MsgTaskExecutor("shutdown-test", 1, false);
        executor.shutdown();
        AtomicBoolean ran = new AtomicBoolean();
        Assert.assertFalse(executor.execute(() -> ran.set(true)));
        Assert.assertFalse(ran.get());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectNonPositiveThreads() {
        new MsgTaskExecutor("invalid-threads-test", 0, true);
    }

    @Test
    public void shouldNotLeakPermitWhenVirtualExecutorIsShutdown() throws Exception {
        Assume.assumeTrue("virtual threads require Java 21+", javaVersion() >= 21);
        MsgTaskExecutor executor = new MsgTaskExecutor("virtual-shutdown-test", 1, true);
        executor.shutdown();
        Assert.assertFalse(executor.execute(() -> {
        }));
    }

    @Test
    public void shouldKeepModeAndRejectWhenExecutorFactoryFails() throws Exception {
        MsgTaskExecutor executor = new MsgTaskExecutor("factory-failure-test", 1, true,
                new MsgTaskExecutor.ExecutorFactory() {
                    @Override
                    public ExecutorService createPlatform(String name, int threads) {
                        return Executors.newSingleThreadExecutor();
                    }

                    @Override
                    public ExecutorService createVirtual() {
                        return Executors.newSingleThreadExecutor();
                    }
                });
        try {
            Assert.assertEquals("virtual", executor.getMode());
            Assert.assertTrue(executor.execute(() -> {
            }));
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void shouldFailFastWhenVirtualThreadsAreUnsupported() {
        Assume.assumeTrue("this case verifies the pre-Java-21 startup failure", javaVersion() < 21);
        String expectedVersion = "java.version=" + System.getProperty("java.version");

        // Startup validation must surface the JVM version instead of silently falling back to platform threads.
        IllegalStateException failure = startupFailure(MsgExecutor::validateVirtualThreadSupport);
        Assert.assertTrue("unexpected failure message: " + failure.getMessage(),
                failure.getMessage().contains(expectedVersion));

        startupFailure(() -> new MsgTaskExecutor("unsupported-jvm-test", 1, true));
        startupFailure(() -> new MsgExecutor("unsupported-jvm-biz-test", 1, 0.5,
                new ThreadContextSwitchStrategy.Default(), true));
    }

    @Test
    public void shouldAcceptVirtualThreadsOnJava21() {
        Assume.assumeTrue("virtual threads require Java 21+", javaVersion() >= 21);
        MsgExecutor.validateVirtualThreadSupport();
        MsgTaskExecutor executor = new MsgTaskExecutor("supported-jvm-test", 1, true);
        try {
            Assert.assertEquals("virtual", executor.getMode());
        } finally {
            executor.shutdown();
        }
    }

    private static IllegalStateException startupFailure(Runnable action) {
        try {
            action.run();
        } catch (IllegalStateException e) {
            return e;
        }
        throw new AssertionError("startup must fail when virtual threads are unavailable");
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static int javaVersion() {
        String version = System.getProperty("java.specification.version");
        if (version.startsWith("1.")) {
            return Integer.parseInt(version.substring(2));
        }
        return Integer.parseInt(version);
    }

    private static boolean isVirtualThread() throws Exception {
        if (javaVersion() < 21) {
            return false;
        }
        Method method = Thread.class.getMethod("isVirtual");
        return (Boolean) method.invoke(Thread.currentThread());
    }

    private static boolean isVirtualThreadQuietly() {
        try {
            return isVirtualThread();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
