package com.netease.nim.camellia.mq.isolation.core;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.core.client.env.ThreadContextSwitchStrategy;
import com.netease.nim.camellia.mq.isolation.core.config.ConsumerConfig;
import com.netease.nim.camellia.mq.isolation.core.config.MqIsolationConfig;
import com.netease.nim.camellia.mq.isolation.core.config.MqIsolationEnv;
import com.netease.nim.camellia.mq.isolation.core.domain.ConsumerContext;
import com.netease.nim.camellia.mq.isolation.core.domain.MqIsolationMsg;
import com.netease.nim.camellia.mq.isolation.core.domain.MqIsolationMsgPacket;
import com.netease.nim.camellia.mq.isolation.core.domain.PacketSerializer;
import com.netease.nim.camellia.mq.isolation.core.executor.MsgExecutor;
import com.netease.nim.camellia.mq.isolation.core.executor.MsgHandler;
import com.netease.nim.camellia.mq.isolation.core.executor.MsgHandlerResult;
import com.netease.nim.camellia.mq.isolation.core.mq.MqInfo;
import com.netease.nim.camellia.mq.isolation.core.mq.MqSender;
import com.netease.nim.camellia.mq.isolation.core.mq.TopicType;
import com.netease.nim.camellia.mq.isolation.core.stats.ConsumerBizStatsCollector;
import com.netease.nim.camellia.mq.isolation.core.stats.ConsumerMonitor;
import com.netease.nim.camellia.tools.executor.CamelliaThreadFactory;
import com.netease.nim.camellia.tools.utils.SysUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/**
 * Created by caojiajun on 2024/2/4
 */
public class CamelliaMqIsolationMsgDispatcher implements MqIsolationMsgDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaMqIsolationMsgDispatcher.class);
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(SysUtils.getCpuNum(),
            new CamelliaThreadFactory("camellia-mq-isolation-dispatcher"));

    private final MsgHandler msgHandler;
    private final MqSender mqSender;
    private final int threads;
    private final double maxPermitPercent;
    private final ConcurrentHashMap<String, MsgExecutor> executorMap = new ConcurrentHashMap<>();
    private final ThreadContextSwitchStrategy strategy;
    private final String namespace;
    private final MqIsolationController controller;
    private final ConsumerBizStatsCollector collector;

    private ConcurrentHashMap<MqInfo, TopicType> topicTypeMap = new ConcurrentHashMap<>();
    private MqIsolationConfig mqIsolationConfig;

    public CamelliaMqIsolationMsgDispatcher(ConsumerConfig config) {
        this.namespace = config.getNamespace();
        this.controller = config.getController();
        this.mqIsolationConfig = controller.getMqIsolationConfig(namespace);
        this.msgHandler = config.getMsgHandler();
        this.threads = config.getThreads();
        this.maxPermitPercent = config.getMaxPermitPercent();
        this.mqSender = config.getMqSender();
        this.strategy = config.getStrategy();
        this.collector = new ConsumerBizStatsCollector(controller, config.getReportIntervalSeconds());
        boolean success = initMqInfoConfig();
        if (!success) {
            throw new IllegalArgumentException("init mq config error");
        }
        scheduler.scheduleAtFixedRate(this::initMqInfoConfig,
                config.getReloadConfigIntervalSeconds(), config.getReloadConfigIntervalSeconds(), TimeUnit.SECONDS);
        ConsumerMonitor.init(MqIsolationEnv.monitorIntervalSeconds);
    }

    @Override
    public CompletableFuture<Boolean> onMsg(MqInfo mqInfo, byte[] data) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            long startTime = System.currentTimeMillis();
            TopicType topicType = topicType(mqInfo);
            MqIsolationMsgPacket packet = PacketSerializer.unmarshal(data);
            if (!packet.getMsg().getNamespace().equals(this.namespace)) {
                throw new IllegalArgumentException("illegal namespace, expect '" + this.namespace + "', actual '" + packet.getMsg().getNamespace() + "'");
            }
            //latency
            long mqLatency = startTime - packet.getMsgPushMqTime();
            long msgLatency = startTime - packet.getMsgCreateTime();
            ConsumerMonitor.latency(mqInfo, topicType, namespace, packet.getMsg().getBizId(), mqLatency, msgLatency);
            //select executor
            ConsumerContext context = newConsumerContext(packet, mqInfo, topicType);
            MsgExecutor executor = selectExecutor(context);
            //try submit executor
            boolean success = executor.submit(topicType, packet.getMsg().getBizId(), () -> {
                try {
                    long handlerBeginTime = System.currentTimeMillis();
                    MsgHandlerResult result;
                    try {
                        //====business handler start====
                        result = msgHandler.onMsg(context);
                        //====business handler end====
                    } catch (Throwable e) {
                        logger.error("msgHandler onMsg error, mqInfo = {}, data = {}", mqInfo, new String(data, StandardCharsets.UTF_8), e);
                        result = MsgHandlerResult.FAILED_WITHOUT_RETRY;
                    }
                    //monitor
                    long spendMs = System.currentTimeMillis() - handlerBeginTime;
                    monitor(packet, result, spendMs);
                    //retry
                    if (result == MsgHandlerResult.FAILED_WITH_RETRY) {
                        retry(packet);
                    }
                    future.complete(true);
                } catch (Throwable e) {
                    logger.error("unknown error, mqInfo = {}, data = {}", mqInfo, new String(data, StandardCharsets.UTF_8), e);
                    future.complete(false);
                }
            });
            //submit failure, send to auto isolation mq
            if (!success) {
                autoIsolation(topicType, packet);
                future.complete(true);
            }
        } catch (Throwable e) {
            logger.error("onMsg error, mqInfo = {}, data = {}", mqInfo, new String(data, StandardCharsets.UTF_8), e);
            future.complete(false);
        }
        return future;
    }

    private void monitor(MqIsolationMsgPacket packet, MsgHandlerResult result, long spendMs) {
        collector.stats(namespace, packet.getMsg().getBizId(), result.isSuccess(), spendMs);
        ConsumerMonitor.spend(namespace, packet.getMsg().getBizId(), result, spendMs);
    }



    private ConsumerContext newConsumerContext(MqIsolationMsgPacket packet, MqInfo mqInfo, TopicType topicType) {
        ConsumerContext context = new ConsumerContext();
        context.setMsg(new MqIsolationMsg(packet.getMsg().getNamespace(), packet.getMsg().getBizId(), packet.getMsg().getMsg()));
        context.setMqInfo(new MqInfo(mqInfo.getMq(), mqInfo.getTopic()));
        context.setTopicType(topicType);
        context.setRetry(packet.getRetry());
        context.setMsgCreateTime(packet.getMsgCreateTime());
        context.setMsgPushMqTime(packet.getMsgPushMqTime());
        return context;
    }

    private MsgExecutor selectExecutor(ConsumerContext context) {
        String name = context.getMqInfo().getMq() + "#" + context.getMqInfo().getTopic();
        MsgExecutor executor = executorMap.get(name);
        if (executor != null) {
            return executor;
        }
        return executorMap.computeIfAbsent(name, k -> new MsgExecutor(name, threads, maxPermitPercent, strategy));
    }

    private void retry(MqIsolationMsgPacket packet) {
        packet.setRetry(packet.getRetry() + 1);
        packet.setMsgPushMqTime(System.currentTimeMillis());
        byte[] data = PacketSerializer.marshal(packet);
        mqSender.send(retryMq(packet), data);
    }

    private MqInfo retryMq(MqIsolationMsgPacket packet) {
        if (packet.getRetry() >= 5) {
            return rand(mqIsolationConfig.getRetryLevel1());
        }
        return rand(mqIsolationConfig.getRetryLevel0());
    }

    private void autoIsolation(TopicType topicType, MqIsolationMsgPacket packet) {
        packet.setMsgPushMqTime(System.currentTimeMillis());
        mqSender.send(autoIsolationMq(topicType), JSONObject.toJSONString(packet).getBytes(StandardCharsets.UTF_8));
    }

    private MqInfo autoIsolationMq(TopicType topicType) {
        if (topicType != TopicType.AUTO_ISOLATION_LEVEL_0 && topicType != TopicType.AUTO_ISOLATION_LEVEL_1) {
            return rand(mqIsolationConfig.getAutoIsolationLevel0());
        }
        return rand(mqIsolationConfig.getAutoIsolationLevel1());
    }

    private MqInfo rand(List<MqInfo> list) {
        if (list.size() == 1) {
            return list.get(0);
        }
        int index = ThreadLocalRandom.current().nextInt(list.size());
        return list.get(index);
    }

    private TopicType topicType(MqInfo mqInfo) {
        TopicType topicType = topicTypeMap.get(mqInfo);
        if (topicType == null) {
            return TopicType.FAST;
        }
        return topicType;
    }

    private boolean initMqInfoConfig() {
        try {
            MqIsolationConfig config = controller.getMqIsolationConfig(namespace);
            ConcurrentHashMap<MqInfo, TopicType> topicTypeMap = new ConcurrentHashMap<>();
            initMqInfo(topicTypeMap, config.getFast(), TopicType.FAST);
            initMqInfo(topicTypeMap, config.getFastError(), TopicType.FAST_ERROR);
            initMqInfo(topicTypeMap, config.getSlow(), TopicType.SLOW);
            initMqInfo(topicTypeMap, config.getSlowError(), TopicType.SLOW_ERROR);
            initMqInfo(topicTypeMap, config.getRetryLevel0(), TopicType.RETRY_LEVEL_0);
            initMqInfo(topicTypeMap, config.getRetryLevel1(), TopicType.RETRY_LEVEL_1);
            initMqInfo(topicTypeMap, config.getAutoIsolationLevel0(), TopicType.AUTO_ISOLATION_LEVEL_0);
            initMqInfo(topicTypeMap, config.getAutoIsolationLevel1(), TopicType.AUTO_ISOLATION_LEVEL_1);
            List<MqIsolationConfig.ManualConfig> manualConfigs = config.getManualConfigs();
            if (manualConfigs != null) {
                Set<MqInfo> mqInfoSet = new HashSet<>();
                for (MqIsolationConfig.ManualConfig manualConfig : manualConfigs) {
                    MqInfo mqInfo = manualConfig.getMqInfo();
                    mqInfoSet.add(mqInfo);
                }
                initMqInfo(topicTypeMap, mqInfoSet, TopicType.MANUAL_ISOLATION);
            }
            this.mqIsolationConfig = config;
            this.topicTypeMap = topicTypeMap;
            return true;
        } catch (Exception e) {
            logger.error("initMqInfoConfig error, namespace = {}", namespace, e);
            return false;
        }
    }

    private void initMqInfo(Map<MqInfo, TopicType> map, Collection<MqInfo> list, TopicType topicType) {
        for (MqInfo mqInfo : list) {
            if (topicType == TopicType.MANUAL_ISOLATION && map.containsKey(mqInfo)) {
                continue;
            } else if (map.containsKey(mqInfo)) {
                throw new IllegalArgumentException("duplicate mq info");
            }
            map.put(mqInfo, topicType);
        }
    }
}
