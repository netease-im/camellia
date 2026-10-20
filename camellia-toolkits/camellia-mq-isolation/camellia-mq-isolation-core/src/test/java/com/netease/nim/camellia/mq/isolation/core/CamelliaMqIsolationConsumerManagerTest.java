package com.netease.nim.camellia.mq.isolation.core;

import com.netease.nim.camellia.mq.isolation.core.config.ConsumerManagerConfig;
import com.netease.nim.camellia.mq.isolation.core.config.ConsumerManagerType;
import com.netease.nim.camellia.mq.isolation.core.config.ConsumerMqInfoConfig;
import com.netease.nim.camellia.mq.isolation.core.config.DispatcherConfig;
import com.netease.nim.camellia.mq.isolation.core.config.MqIsolationConfig;
import com.netease.nim.camellia.mq.isolation.core.domain.ConsumerHeartbeat;
import com.netease.nim.camellia.mq.isolation.core.domain.SenderHeartbeat;
import com.netease.nim.camellia.mq.isolation.core.executor.MsgHandlerResult;
import com.netease.nim.camellia.mq.isolation.core.mq.Consumer;
import com.netease.nim.camellia.mq.isolation.core.mq.MqInfo;
import com.netease.nim.camellia.mq.isolation.core.stats.model.ConsumerBizStatsRequest;
import com.netease.nim.camellia.mq.isolation.core.stats.model.SenderBizStatsRequest;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class CamelliaMqIsolationConsumerManagerTest {

    @Test
    public void shouldRetryConsumerThatFailedToStop() {
        AtomicInteger startCalls = new AtomicInteger();
        AtomicInteger stopCalls = new AtomicInteger();
        CamelliaMqIsolationConsumerManager manager = new CamelliaMqIsolationConsumerManager(config(startCalls, stopCalls));
        manager.start();

        try {
            manager.stop();
            Assert.fail("first stop should report consumer failure");
        } catch (IllegalStateException expected) {
            Assert.assertTrue(expected.getMessage().contains("stop consumer error"));
        }
        Assert.assertEquals(1, startCalls.get());
        Assert.assertEquals(1, stopCalls.get());

        manager.stop();
        Assert.assertEquals("failed consumer must be stopped again", 2, stopCalls.get());
        try {
            manager.stop();
            Assert.fail("completed stop should still reject duplicate calls");
        } catch (IllegalStateException expected) {
            Assert.assertTrue(expected.getMessage().contains("stop twice"));
        }
    }

    private static ConsumerManagerConfig config(AtomicInteger startCalls, AtomicInteger stopCalls) {
        MqInfo mqInfo = new MqInfo("mq", "topic");
        MqIsolationConfig isolationConfig = new MqIsolationConfig();
        isolationConfig.setNamespace("test");
        isolationConfig.setConsumerStatsIntervalSeconds(3600);
        isolationConfig.setLevelInfoList(Collections.emptyList());
        isolationConfig.setErrorLevelInfoList(Collections.emptyList());
        isolationConfig.setRetryLevel0(Collections.emptyList());
        isolationConfig.setRetryLevel1(Collections.emptyList());
        isolationConfig.setAutoIsolationLevel0(Collections.emptyList());
        isolationConfig.setAutoIsolationLevel1(Collections.emptyList());
        isolationConfig.setManualConfigs(Collections.emptyList());

        MqIsolationController controller = new MqIsolationController() {
            @Override
            public MqIsolationConfig getMqIsolationConfig(String namespace) {
                return isolationConfig;
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
        DispatcherConfig dispatcherConfig = new DispatcherConfig();
        dispatcherConfig.setNamespace("test");
        dispatcherConfig.setController(controller);
        dispatcherConfig.setMsgHandler(context -> MsgHandlerResult.SUCCESS);
        dispatcherConfig.setMqSender((target, data) -> null);
        dispatcherConfig.setThreads(1);
        dispatcherConfig.setReloadConfigIntervalSeconds(3600);

        ConsumerMqInfoConfig mqInfoConfig = new ConsumerMqInfoConfig();
        mqInfoConfig.setType(ConsumerManagerType.specify_mq_info);
        mqInfoConfig.setSpecifyMqInfoSet(Collections.singleton(mqInfo));

        ConsumerManagerConfig config = new ConsumerManagerConfig();
        config.setDispatcherConfig(dispatcherConfig);
        config.setMqInfoConfig(mqInfoConfig);
        config.setConsumerBuilder((target, dispatcher) -> new Consumer(target, dispatcher) {
            @Override
            public void start() {
                startCalls.incrementAndGet();
            }

            @Override
            public void stop() {
                if (stopCalls.incrementAndGet() == 1) {
                    throw new IllegalStateException("temporary stop failure");
                }
            }
        });
        return config;
    }
}
