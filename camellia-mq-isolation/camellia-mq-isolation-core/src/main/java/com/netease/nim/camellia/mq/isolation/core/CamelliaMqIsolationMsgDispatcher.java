package com.netease.nim.camellia.mq.isolation.core;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.core.client.env.ThreadContextSwitchStrategy;
import com.netease.nim.camellia.mq.isolation.core.config.*;
import com.netease.nim.camellia.mq.isolation.core.domain.ConsumerContext;
import com.netease.nim.camellia.mq.isolation.core.domain.MqIsolationMsg;
import com.netease.nim.camellia.mq.isolation.core.domain.MqIsolationMsgPacket;
import com.netease.nim.camellia.mq.isolation.core.domain.PacketSerializer;
import com.netease.nim.camellia.mq.isolation.core.env.MqIsolationEnv;
import com.netease.nim.camellia.mq.isolation.core.executor.MsgExecutor;
import com.netease.nim.camellia.mq.isolation.core.executor.MsgHandler;
import com.netease.nim.camellia.mq.isolation.core.executor.MsgHandlerResult;
import com.netease.nim.camellia.mq.isolation.core.mq.MqInfo;
import com.netease.nim.camellia.mq.isolation.core.mq.MqSender;
import com.netease.nim.camellia.mq.isolation.core.mq.TopicType;
import com.netease.nim.camellia.mq.isolation.core.stats.ConsumerBizStatsCollector;
import com.netease.nim.camellia.mq.isolation.core.stats.ConsumerMonitor;
import com.netease.nim.camellia.tools.cache.CamelliaLocalCache;
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

    private final DispatcherConfig dispatcherConfig;
    private final MsgHandler msgHandler;
    private final MqSender mqSender;
    private final int threads;
    private final double maxPermitPercent;
    private final ConcurrentHashMap<String, MsgExecutor> executorMap = new ConcurrentHashMap<>();
    private final CamelliaLocalCache cache = new CamelliaLocalCache();
    private final ThreadContextSwitchStrategy strategy;
    private final String namespace;
    private final MqIsolationController controller;
    private final ConsumerBizStatsCollector collector;

    private ConcurrentHashMap<MqInfo, TopicType> topicTypeMap = new ConcurrentHashMap<>();
    private MqIsolationConfig mqIsolationConfig;

    public CamelliaMqIsolationMsgDispatcher(DispatcherConfig config) {
        this.dispatcherConfig = config;
        this.namespace = config.getNamespace();
        this.controller = config.getController();
        this.mqIsolationConfig = controller.getMqIsolationConfig(namespace);
        this.msgHandler = config.getMsgHandler();
        this.threads = config.getThreads();
        this.maxPermitPercent = config.getMaxPermitPercent();
        this.mqSender = config.getMqSender();
        this.strategy = config.getStrategy();
        this.collector = new ConsumerBizStatsCollector(controller);
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
            MqIsolationMsgPacket packet = PacketSerializer.unmarshal(data);
            TopicInfo topicInfo = topicInfo(mqInfo, packet);
            if (!packet.getMsg().getNamespace().equals(this.namespace)) {
                throw new IllegalArgumentException("illegal namespace, expect '" + this.namespace + "', actual '" + packet.getMsg().getNamespace() + "'");
            }
            //latency
            long mqLatency = startTime - packet.getMsgPushMqTime();
            long msgLatency = startTime - packet.getMsgCreateTime();
            ConsumerMonitor.latency(mqInfo, topicInfo.topicType, namespace, packet.getMsg().getBizId(), mqLatency, msgLatency);
            //select executor
            ConsumerContext context = newConsumerContext(packet, mqInfo, topicInfo.topicType);
            MsgExecutor executor = selectExecutor(context);
            //try submit executor
            String bizId = packet.getMsg().getBizId();
            boolean success = executor.submit(bizId, topicInfo.autoIsolation, () -> {
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
                        sendRetryMq(packet);
                    }
                    future.complete(true);
                } catch (Throwable e) {
                    logger.error("unknown error, mqInfo = {}, data = {}", mqInfo, new String(data, StandardCharsets.UTF_8), e);
                    future.complete(false);
                }
            });
            //submit failure, send to auto isolation mq
            if (!success) {
                sendAutoIsolationMq(topicInfo.topicType, packet);
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

    private static class TopicInfo {
        final TopicType topicType;
        final boolean autoIsolation;

        public TopicInfo(TopicType topicType, boolean autoIsolation) {
            this.topicType = topicType;
            this.autoIsolation = autoIsolation;
        }
    }

    private TopicInfo topicInfo(MqInfo mqInfo, MqIsolationMsgPacket packet) {
        String bizId = packet.getMsg().getBizId();
        String tag = "auto";
        TopicInfo topicInfo = cache.get(tag, bizId, TopicInfo.class);
        if (topicInfo != null) {
            return topicInfo;
        }
        List<ManualConfig> manualConfigs = mqIsolationConfig.getManualConfigs();
        for (ManualConfig manualConfig : manualConfigs) {
            MatchType matchType = manualConfig.getMatchType();
            if (matchType == MatchType.exact_match) {
                if (manualConfig.getBizId().equals(bizId)) {
                    if (mqInfo.equals(manualConfig.getMqInfo())) {
                        topicInfo = new TopicInfo(TopicType.MANUAL_ISOLATION, manualConfig.isAutoIsolation());
                        break;
                    }
                }
            } else if (matchType == MatchType.prefix_match) {
                if (bizId.startsWith(manualConfig.getBizId())) {
                    if (mqInfo.equals(manualConfig.getMqInfo())) {
                        topicInfo = new TopicInfo(TopicType.MANUAL_ISOLATION, manualConfig.isAutoIsolation());
                        break;
                    }
                }
            }
        }
        if (topicInfo == null) {
            TopicType topicType = topicTypeMap.get(mqInfo);
            if (topicType == null) {
                topicType = TopicType.NORMAL;
            }
            topicInfo = new TopicInfo(topicType, topicType.isAutoIsolation());
        }
        cache.put(tag, bizId, topicInfo, -1);
        return topicInfo;
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

    private void sendRetryMq(MqIsolationMsgPacket packet) {
        packet.setRetry(packet.getRetry() + 1);
        packet.setMsgPushMqTime(System.currentTimeMillis());
        byte[] data = PacketSerializer.marshal(packet);
        mqSender.send(selectRetryMq(packet), data);
    }

    private MqInfo selectRetryMq(MqIsolationMsgPacket packet) {
        if (packet.getRetry() >= dispatcherConfig.getRetryLevelThreshold()) {
            return randMq(mqIsolationConfig.getRetryLevel1());
        }
        return randMq(mqIsolationConfig.getRetryLevel0());
    }

    private void sendAutoIsolationMq(TopicType topicType, MqIsolationMsgPacket packet) {
        packet.setMsgPushMqTime(System.currentTimeMillis());
        mqSender.send(selectAutoIsolationMq(topicType), JSONObject.toJSONString(packet).getBytes(StandardCharsets.UTF_8));
    }

    private MqInfo selectAutoIsolationMq(TopicType topicType) {
        if (topicType != TopicType.AUTO_ISOLATION_LEVEL_0 && topicType != TopicType.AUTO_ISOLATION_LEVEL_1) {
            return randMq(mqIsolationConfig.getAutoIsolationLevel0());
        }
        return randMq(mqIsolationConfig.getAutoIsolationLevel1());
    }

    private MqInfo randMq(List<MqInfo> list) {
        if (list.size() == 1) {
            return list.get(0);
        }
        int index = ThreadLocalRandom.current().nextInt(list.size());
        return list.get(index);
    }

    private boolean initMqInfoConfig() {
        try {
            MqIsolationConfig config = controller.getMqIsolationConfig(namespace);
            this.topicTypeMap = MqIsolationConfigUtils.topicTypeMap(config);
            this.mqIsolationConfig = config;
            this.cache.clear();
            return true;
        } catch (Exception e) {
            logger.error("initMqInfoConfig error, namespace = {}", namespace, e);
            return false;
        }
    }
}
