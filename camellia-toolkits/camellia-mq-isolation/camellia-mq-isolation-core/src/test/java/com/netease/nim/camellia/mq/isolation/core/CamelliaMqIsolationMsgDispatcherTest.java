package com.netease.nim.camellia.mq.isolation.core;

import com.netease.nim.camellia.core.client.env.ThreadContextSwitchStrategy;
import com.netease.nim.camellia.mq.isolation.core.config.DispatcherConfig;
import com.netease.nim.camellia.mq.isolation.core.config.MqIsolationConfig;
import com.netease.nim.camellia.mq.isolation.core.domain.ConsumerContext;
import com.netease.nim.camellia.mq.isolation.core.domain.ConsumerHeartbeat;
import com.netease.nim.camellia.mq.isolation.core.domain.SenderHeartbeat;
import com.netease.nim.camellia.mq.isolation.core.executor.MsgExecutor;
import com.netease.nim.camellia.mq.isolation.core.executor.MsgHandlerResult;
import com.netease.nim.camellia.mq.isolation.core.mq.MqInfo;
import com.netease.nim.camellia.mq.isolation.core.stats.model.ConsumerBizStatsRequest;
import com.netease.nim.camellia.mq.isolation.core.stats.model.SenderBizStatsRequest;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by caojiajun on 2026/10/20.
 */
public class CamelliaMqIsolationMsgDispatcherTest {

    @Test
    public void shouldShutdownExecutorCreatedDuringShutdown() throws Exception {
        CountDownLatch creating = new CountDownLatch(1);
        CountDownLatch allowCreate = new CountDownLatch(1);
        AtomicReference<MsgExecutor> created = new AtomicReference<>();
        CamelliaMqIsolationMsgDispatcher dispatcher = new CamelliaMqIsolationMsgDispatcher(config(), name -> {
            creating.countDown();
            await(allowCreate);
            MsgExecutor executor = new MsgExecutor(name, 1, 0.5, new ThreadContextSwitchStrategy.Default());
            created.set(executor);
            return executor;
        });
        try {
            Thread selector = new Thread(() -> selectExecutor(dispatcher, "mq", "topic"));
            selector.start();
            Assert.assertTrue("should enter executor creation", creating.await(5, TimeUnit.SECONDS));

            // 在 executor 创建过程中触发 shutdown：修复后 shutdown 需要等创建完成并把新 executor 一起关闭
            Thread shutdown = new Thread(dispatcher::shutdown);
            shutdown.start();
            allowCreate.countDown();
            selector.join(5000);
            shutdown.join(5000);

            Assert.assertNotNull(created.get());
            Assert.assertFalse("executor created during shutdown should be shut down",
                    created.get().submit("biz", false, () -> {}));
        } finally {
            dispatcher.shutdown();
        }
    }

    @Test
    public void shouldNotCreateExecutorAfterShutdown() {
        AtomicInteger created = new AtomicInteger();
        CamelliaMqIsolationMsgDispatcher dispatcher = new CamelliaMqIsolationMsgDispatcher(config(), name -> {
            created.incrementAndGet();
            return new MsgExecutor(name, 1, 0.5, new ThreadContextSwitchStrategy.Default());
        });
        try {
            dispatcher.shutdown();
            Assert.assertNull("selectExecutor should return null after shutdown", selectExecutor(dispatcher, "mq", "topic"));
            Assert.assertEquals("no executor should be created after shutdown", 0, created.get());
        } finally {
            dispatcher.shutdown();
        }
    }

    @Test
    public void shouldCreateOnlyOneExecutorPerName() throws Exception {
        AtomicInteger created = new AtomicInteger();
        CamelliaMqIsolationMsgDispatcher dispatcher = new CamelliaMqIsolationMsgDispatcher(config(), name -> {
            created.incrementAndGet();
            return new MsgExecutor(name, 1, 0.5, new ThreadContextSwitchStrategy.Default());
        });
        try {
            int threadCount = 8;
            CountDownLatch start = new CountDownLatch(1);
            Set<MsgExecutor> results = Collections.synchronizedSet(
                    Collections.newSetFromMap(new IdentityHashMap<MsgExecutor, Boolean>()));
            List<Thread> threads = new ArrayList<>();
            for (int i = 0; i < threadCount; i++) {
                Thread thread = new Thread(() -> {
                    await(start);
                    results.add(selectExecutor(dispatcher, "mq", "topic"));
                });
                thread.start();
                threads.add(thread);
            }
            start.countDown();
            for (Thread thread : threads) {
                thread.join(5000);
            }
            Assert.assertEquals("only one executor should be created", 1, created.get());
            Assert.assertEquals("all callers should share the same executor", 1, results.size());
        } finally {
            dispatcher.shutdown();
        }
    }

    private static MsgExecutor selectExecutor(CamelliaMqIsolationMsgDispatcher dispatcher, String mq, String topic) {
        ConsumerContext context = new ConsumerContext();
        context.setMqInfo(new MqInfo(mq, topic));
        return dispatcher.selectExecutor(context);
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static DispatcherConfig config() {
        final MqIsolationConfig mqIsolationConfig = new MqIsolationConfig();
        mqIsolationConfig.setNamespace("test");
        mqIsolationConfig.setLevelInfoList(Collections.emptyList());
        mqIsolationConfig.setErrorLevelInfoList(Collections.emptyList());
        mqIsolationConfig.setRetryLevel0(Collections.emptyList());
        mqIsolationConfig.setRetryLevel1(Collections.emptyList());
        mqIsolationConfig.setAutoIsolationLevel0(Collections.emptyList());
        mqIsolationConfig.setAutoIsolationLevel1(Collections.emptyList());
        mqIsolationConfig.setManualConfigs(Collections.emptyList());
        MqIsolationController controller = new MqIsolationController() {
            @Override
            public MqIsolationConfig getMqIsolationConfig(String namespace) {
                return mqIsolationConfig;
            }

            @Override
            public void reportConsumerBizStats(ConsumerBizStatsRequest request) {
            }

            @Override
            public void reportSenderBizStats(SenderBizStatsRequest request) {
            }

            @Override
            public List<MqInfo> selectMqInfo(String namespace, String bizId) {
                return Collections.emptyList();
            }

            @Override
            public void consumerHeartbeat(ConsumerHeartbeat heartbeat) {
            }

            @Override
            public void senderHeartbeat(SenderHeartbeat heartbeat) {
            }
        };
        DispatcherConfig config = new DispatcherConfig();
        config.setNamespace("test");
        config.setController(controller);
        config.setMsgHandler(context -> MsgHandlerResult.SUCCESS);
        config.setMqSender((mqInfo, data) -> null);
        config.setThreads(1);
        config.setReloadConfigIntervalSeconds(3600);
        return config;
    }
}
