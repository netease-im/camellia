package com.netease.nim.camellia.mq.isolation.core;

import com.netease.nim.camellia.mq.isolation.core.config.ConsumerManagerConfig;
import com.netease.nim.camellia.mq.isolation.core.config.ConsumerManagerType;
import com.netease.nim.camellia.mq.isolation.core.config.ManualConfig;
import com.netease.nim.camellia.mq.isolation.core.config.MqIsolationConfig;
import com.netease.nim.camellia.mq.isolation.core.domain.ConsumerHeartbeat;
import com.netease.nim.camellia.mq.isolation.core.env.MqIsolationEnv;
import com.netease.nim.camellia.mq.isolation.core.mq.Consumer;
import com.netease.nim.camellia.mq.isolation.core.mq.MqInfo;
import com.netease.nim.camellia.mq.isolation.core.mq.TopicType;
import com.netease.nim.camellia.tools.executor.CamelliaThreadFactory;
import com.netease.nim.camellia.tools.utils.SysUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by caojiajun on 2024/2/7
 */
public class CamelliaMqIsolationConsumerManager implements MqIsolationConsumerManager {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaMqIsolationConsumerManager.class);
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(SysUtils.getCpuNum(),
            new CamelliaThreadFactory("camellia-mq-isolation-consumer-manager"));

    private final ConsumerManagerConfig config;
    private final MqIsolationController controller;
    private final String namespace;
    private final CamelliaMqIsolationMsgDispatcher dispatcher;
    private final ConcurrentHashMap<MqInfo, Consumer> instanceMap = new ConcurrentHashMap<>();
    private final ReentrantLock lock = new ReentrantLock();
    private final AtomicBoolean initOk = new AtomicBoolean(false);
    private final AtomicBoolean stop = new AtomicBoolean(false);
    private ScheduledFuture<?> future;

    public CamelliaMqIsolationConsumerManager(ConsumerManagerConfig config) {
        this.config = config;
        if (config.getConsumerBuilder() == null) {
            throw new IllegalArgumentException("consumer builder is null");
        }
        this.namespace = config.getDispatcherConfig().getNamespace();
        this.controller = config.getDispatcherConfig().getController();
        this.dispatcher = new CamelliaMqIsolationMsgDispatcher(config.getDispatcherConfig());
    }

    /**
     * 启动
     */
    @Override
    public void start() {
        if (initOk.compareAndSet(false, true)) {
            boolean success = initConsumers();
            if (!success) {
                throw new IllegalArgumentException("mq isolation consumer start error");
            }
            future = scheduler.scheduleAtFixedRate(this::initConsumers,
                    config.getReloadConsumerIntervalSeconds(), config.getReloadConsumerIntervalSeconds(), TimeUnit.SECONDS);
            logger.info("mq isolation consumer manager start success");
        } else {
            throw new IllegalStateException("mq isolation consumer start twice!");
        }
    }

    /**
     * 关闭
     */
    @Override
    public void stop() {
        if (stop.get()) {
            throw new IllegalStateException("mq isolation consumer stop twice!");
        }
        if (!initOk.get()) {
            throw new IllegalStateException("mq isolation consumer not start!");
        }
        lock.lock();
        try {
            if (future != null) {
                future.cancel(false);
            }
            Set<MqInfo> set = new HashSet<>(instanceMap.keySet());
            for (MqInfo mqInfo: set) {
                logger.info("try stop consumer, namespace = {}, mqInfo = {}", namespace, mqInfo);
                Consumer instance = instanceMap.remove(mqInfo);
                if (instance != null) {
                    instance.stop();
                    logger.info("stop consumer success, namespace = {}, mqInfo = {}", namespace, mqInfo);
                }
            }
            logger.info("mq isolation consumer manager stop success");
            stop.set(true);
        } finally {
            lock.unlock();
        }
    }


    //启动消费线程
    private boolean initConsumers() {
        if (stop.get()) {
            return false;
        }
        lock.lock();
        try {
            ConsumerManagerType type = config.getType();
            MqIsolationConfig mqIsolationConfig = controller.getMqIsolationConfig(namespace);
            Set<MqInfo> set = new HashSet<>();
            if (type == ConsumerManagerType.all) {
                set.addAll(mqIsolationConfig.getFast());
                set.addAll(mqIsolationConfig.getFastError());
                set.addAll(mqIsolationConfig.getSlow());
                set.addAll(mqIsolationConfig.getSlowError());
                set.addAll(mqIsolationConfig.getRetryLevel0());
                set.addAll(mqIsolationConfig.getRetryLevel1());
                set.addAll(mqIsolationConfig.getAutoIsolationLevel0());
                set.addAll(mqIsolationConfig.getAutoIsolationLevel1());
                List<ManualConfig> manualConfigs = mqIsolationConfig.getManualConfigs();
                if (manualConfigs != null) {
                    for (ManualConfig manualConfig : manualConfigs) {
                        set.add(manualConfig.getMqInfo());
                    }
                }
            } else if (type == ConsumerManagerType.all_exclude_topic_type) {
                Set<TopicType> excludeTopicTypeSet = config.getExcludeTopicTypeSet();
                if (excludeTopicTypeSet != null && !excludeTopicTypeSet.isEmpty()) {
                    if (!excludeTopicTypeSet.contains(TopicType.FAST)) {
                        set.addAll(mqIsolationConfig.getFast());
                    }
                    if (!excludeTopicTypeSet.contains(TopicType.FAST_ERROR)) {
                        set.addAll(mqIsolationConfig.getFastError());
                    }
                    if (!excludeTopicTypeSet.contains(TopicType.SLOW)) {
                        set.addAll(mqIsolationConfig.getSlow());
                    }
                    if (!excludeTopicTypeSet.contains(TopicType.SLOW_ERROR)) {
                        set.addAll(mqIsolationConfig.getSlowError());
                    }
                    if (!excludeTopicTypeSet.contains(TopicType.RETRY_LEVEL_0)) {
                        set.addAll(mqIsolationConfig.getRetryLevel0());
                    }
                    if (!excludeTopicTypeSet.contains(TopicType.RETRY_LEVEL_1)) {
                        set.addAll(mqIsolationConfig.getRetryLevel1());
                    }
                    if (!excludeTopicTypeSet.contains(TopicType.AUTO_ISOLATION_LEVEL_0)) {
                        set.addAll(mqIsolationConfig.getAutoIsolationLevel0());
                    }
                    if (!excludeTopicTypeSet.contains(TopicType.AUTO_ISOLATION_LEVEL_1)) {
                        set.addAll(mqIsolationConfig.getAutoIsolationLevel1());
                    }
                    if (!excludeTopicTypeSet.contains(TopicType.MANUAL_ISOLATION)) {
                        List<ManualConfig> manualConfigs = mqIsolationConfig.getManualConfigs();
                        if (manualConfigs != null) {
                            for (ManualConfig manualConfig : manualConfigs) {
                                set.add(manualConfig.getMqInfo());
                            }
                        }
                    }
                }
            } else if (type == ConsumerManagerType.all_exclude_mq_info) {
                set.addAll(mqIsolationConfig.getFast());
                set.addAll(mqIsolationConfig.getFastError());
                set.addAll(mqIsolationConfig.getSlow());
                set.addAll(mqIsolationConfig.getSlowError());
                set.addAll(mqIsolationConfig.getRetryLevel0());
                set.addAll(mqIsolationConfig.getRetryLevel1());
                set.addAll(mqIsolationConfig.getAutoIsolationLevel0());
                set.addAll(mqIsolationConfig.getAutoIsolationLevel1());
                List<ManualConfig> manualConfigs = mqIsolationConfig.getManualConfigs();
                if (manualConfigs != null) {
                    for (ManualConfig manualConfig : manualConfigs) {
                        set.add(manualConfig.getMqInfo());
                    }
                }
                Set<MqInfo> excludeMqInfoSet = config.getExcludeMqInfoSet();
                if (excludeMqInfoSet != null && !excludeMqInfoSet.isEmpty()) {
                    set.removeAll(excludeMqInfoSet);
                }
            } else if (type == ConsumerManagerType.specify_topic_type) {
                Set<TopicType> specifyTopicTypeSet = config.getSpecifyTopicTypeSet();
                if (specifyTopicTypeSet != null && !specifyTopicTypeSet.isEmpty()) {
                    if (specifyTopicTypeSet.contains(TopicType.FAST)) {
                        set.addAll(mqIsolationConfig.getFast());
                    }
                    if (specifyTopicTypeSet.contains(TopicType.FAST_ERROR)) {
                        set.addAll(mqIsolationConfig.getFastError());
                    }
                    if (specifyTopicTypeSet.contains(TopicType.SLOW)) {
                        set.addAll(mqIsolationConfig.getSlow());
                    }
                    if (specifyTopicTypeSet.contains(TopicType.SLOW_ERROR)) {
                        set.addAll(mqIsolationConfig.getSlowError());
                    }
                    if (specifyTopicTypeSet.contains(TopicType.RETRY_LEVEL_0)) {
                        set.addAll(mqIsolationConfig.getRetryLevel0());
                    }
                    if (specifyTopicTypeSet.contains(TopicType.RETRY_LEVEL_1)) {
                        set.addAll(mqIsolationConfig.getRetryLevel1());
                    }
                    if (specifyTopicTypeSet.contains(TopicType.AUTO_ISOLATION_LEVEL_0)) {
                        set.addAll(mqIsolationConfig.getAutoIsolationLevel0());
                    }
                    if (specifyTopicTypeSet.contains(TopicType.AUTO_ISOLATION_LEVEL_1)) {
                        set.addAll(mqIsolationConfig.getAutoIsolationLevel1());
                    }
                    if (specifyTopicTypeSet.contains(TopicType.MANUAL_ISOLATION)) {
                        List<ManualConfig> manualConfigs = mqIsolationConfig.getManualConfigs();
                        if (manualConfigs != null) {
                            for (ManualConfig manualConfig : manualConfigs) {
                                set.add(manualConfig.getMqInfo());
                            }
                        }
                    }
                }
            } else if (type == ConsumerManagerType.specify_mq_info) {
                Set<MqInfo> specifyMqInfoSet = config.getSpecifyMqInfoSet();
                if (specifyMqInfoSet != null) {
                    set.addAll(specifyMqInfoSet);
                }
            } else {
                throw new IllegalArgumentException("illegal ConsumerManagerType");
            }
            for (MqInfo mqInfo : set) {
                if (instanceMap.containsKey(mqInfo)) {
                    continue;
                }
                try {
                    logger.info("try start consumer, namespace = {}, mqInfo = {}", namespace, mqInfo);
                    Consumer instance = config.getConsumerBuilder().newConsumer(mqInfo, dispatcher);
                    instance.start();
                    logger.info("start consumer success, namespace = {}, mqInfo = {}", namespace, mqInfo);
                    instanceMap.put(mqInfo, instance);
                } catch (Exception e) {
                    logger.error("start consumer error, namespace = {}, mqInfo = {}", namespace, mqInfo, e);
                    throw new IllegalStateException(e);
                }
            }
            return true;
        } catch (Throwable e) {
            logger.error("initConsumers error, namespace = {}", namespace, e);
            return false;
        } finally {
            try {
                for (MqInfo mqInfo : instanceMap.keySet()) {
                    ConsumerHeartbeat heartbeat = new ConsumerHeartbeat();
                    heartbeat.setInstanceId(MqIsolationEnv.instanceId);
                    heartbeat.setHost(MqIsolationEnv.host);
                    heartbeat.setMqInfo(mqInfo);
                    heartbeat.setNamespace(namespace);
                    heartbeat.setTimestamp(System.currentTimeMillis());
                    controller.consumerHeartbeat(heartbeat);
                }
            } catch (Exception e) {
                logger.error("heartbeat to controller error, namespace = {}", namespace, e);
            }
            lock.unlock();
        }
    }
}
