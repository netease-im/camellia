package com.netease.nim.camellia.mq.isolation;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.core.client.env.ThreadContextSwitchStrategy;
import com.netease.nim.camellia.mq.isolation.domain.ConsumerContext;
import com.netease.nim.camellia.mq.isolation.domain.MqIsolationMsg;
import com.netease.nim.camellia.mq.isolation.domain.MqIsolationMsgPacket;
import com.netease.nim.camellia.mq.isolation.domain.PacketSerializer;
import com.netease.nim.camellia.mq.isolation.executor.MsgExecutor;
import com.netease.nim.camellia.mq.isolation.executor.MsgHandler;
import com.netease.nim.camellia.mq.isolation.executor.MsgHandlerResult;
import com.netease.nim.camellia.mq.isolation.mq.MqInfo;
import com.netease.nim.camellia.mq.isolation.mq.MqInfoConfig;
import com.netease.nim.camellia.mq.isolation.mq.MqSender;
import com.netease.nim.camellia.mq.isolation.mq.TopicType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by caojiajun on 2024/2/4
 */
public class CamelliaMqIsolationConsumer implements MqIsolationConsumer {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaMqIsolationConsumer.class);
    private final MsgHandler msgHandler;
    private final MqSender mqSender;
    private final int threads;
    private final ConcurrentHashMap<String, MsgExecutor> executorMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<MqInfo, TopicType> topicTypeMap = new ConcurrentHashMap<>();
    private final MqInfoConfig mqInfoConfig;
    private final ThreadContextSwitchStrategy strategy;

    public CamelliaMqIsolationConsumer(ConsumerConfig config) {
        this.mqInfoConfig = config.getMqInfoConfig();
        this.msgHandler = config.getMsgHandler();
        this.threads = config.getThreads();
        this.mqSender = config.getMqSender();
        this.strategy = config.getStrategy();
        initMqInfo(mqInfoConfig.getFast(), TopicType.FAST);
        initMqInfo(mqInfoConfig.getFastError(), TopicType.FAST_ERROR);
        initMqInfo(mqInfoConfig.getSlow(), TopicType.SLOW);
        initMqInfo(mqInfoConfig.getSlowError(), TopicType.SLOW_ERROR);
        initMqInfo(mqInfoConfig.getRetryLevel0(), TopicType.RETRY_LEVEL_0);
        initMqInfo(mqInfoConfig.getRetryLevel1(), TopicType.RETRY_LEVEL_1);
        initMqInfo(mqInfoConfig.getAutoIsolationLevel0(), TopicType.AUTO_ISOLATION_LEVEL_0);
        initMqInfo(mqInfoConfig.getAutoIsolationLevel1(), TopicType.AUTO_ISOLATION_LEVEL_1);
        initMqInfo(mqInfoConfig.getManualIsolation(), TopicType.MANUAL_ISOLATION);
    }

    @Override
    public CompletableFuture<Boolean> onMsg(MqInfo mqInfo, byte[] data) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            long startTime = System.currentTimeMillis();
            TopicType topicType = topicType(mqInfo);
            MqIsolationMsgPacket packet = PacketSerializer.unmarshal(data);
            //latency
            long mqLatency = startTime - packet.getMsgPushMqTime();
            long msgLatency = startTime - packet.getMsgCreateTime();
            latency(mqInfo, topicType, mqLatency, msgLatency);
            //select executor
            ConsumerContext context = newConsumerContext(packet, mqInfo, topicType);
            MsgExecutor executor = selectExecutor(context);
            //try submit executor
            boolean success = executor.submit(topicType, packet.getMsg().getBizId(), () -> {
                long handlerBeginTime = System.currentTimeMillis();
                MsgHandlerResult result;
                try {
                    //====business handler start====
                    result = msgHandler.onMsg(context);
                    //====business handler end====
                } catch (Throwable e) {
                    logger.error("unknown error, mqInfo = {}, data = {}", mqInfo, new String(data, StandardCharsets.UTF_8), e);
                    result = MsgHandlerResult.FAILED_WITHOUT_RETRY;
                }
                //monitor
                long latencyMs = System.currentTimeMillis() - packet.getMsgCreateTime();
                long spendMs = System.currentTimeMillis() - handlerBeginTime;
                monitor(packet, result, latencyMs, spendMs);
                if (result == MsgHandlerResult.FAILED_WITH_RETRY) {
                    retry(packet);
                }
                future.complete(true);
            });
            //submit failure, send to auto isolation mq
            if (!success) {
                autoIsolation(topicType, packet);
                future.complete(true);
            }
        } catch (Throwable e) {
            logger.error("onMsg error", e);
            future.complete(false);
        }
        return future;
    }

    private void latency(MqInfo mqInfo, TopicType topicType, long mqLatency, long msgLatency) {
        //todo
    }

    private void monitor(MqIsolationMsgPacket packet, MsgHandlerResult result, long latencyMs, long spendMs) {
        //todo
    }

    private TopicType topicType(MqInfo mqInfo) {
        TopicType topicType = topicTypeMap.get(mqInfo);
        if (topicType == null) {
            throw new IllegalArgumentException("unknown mqInfo = " + mqInfo);
        }
        return topicType;
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
        return executorMap.computeIfAbsent(name, k -> new MsgExecutor(name, threads, strategy));
    }

    private void retry(MqIsolationMsgPacket packet) {
        packet.setRetry(packet.getRetry() + 1);
        packet.setMsgPushMqTime(System.currentTimeMillis());
        byte[] data = PacketSerializer.marshal(packet);
        mqSender.send(retryMq(packet), data);
    }

    private MqInfo retryMq(MqIsolationMsgPacket packet) {
        if (packet.getRetry() >= 5) {
            return rand(mqInfoConfig.getRetryLevel1());
        }
        return rand(mqInfoConfig.getRetryLevel0());
    }

    private void autoIsolation(TopicType topicType, MqIsolationMsgPacket packet) {
        packet.setMsgPushMqTime(System.currentTimeMillis());
        mqSender.send(autoIsolationMq(topicType), JSONObject.toJSONString(packet).getBytes(StandardCharsets.UTF_8));
    }

    private MqInfo autoIsolationMq(TopicType topicType) {
        if (topicType != TopicType.AUTO_ISOLATION_LEVEL_0 && topicType != TopicType.AUTO_ISOLATION_LEVEL_1) {
            return rand(mqInfoConfig.getAutoIsolationLevel0());
        }
        return rand(mqInfoConfig.getAutoIsolationLevel1());
    }

    private MqInfo rand(List<MqInfo> list) {
        if (list.size() == 1) {
            return list.get(0);
        }
        int index = ThreadLocalRandom.current().nextInt(list.size());
        return list.get(index);
    }

    private void initMqInfo(List<MqInfo> list, TopicType topicType) {
        for (MqInfo mqInfo : list) {
            if (topicTypeMap.containsKey(mqInfo)) {
                throw new IllegalArgumentException("duplicate mq info");
            }
            topicTypeMap.put(mqInfo, topicType);
        }
    }
}
