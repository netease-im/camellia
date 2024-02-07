package com.netease.nim.camellia.mq.isolation.core;

import com.netease.nim.camellia.mq.isolation.core.config.ConsumerManagerConfig;
import com.netease.nim.camellia.mq.isolation.core.config.ConsumerManagerType;
import com.netease.nim.camellia.mq.isolation.core.config.MqIsolationConfig;
import com.netease.nim.camellia.mq.isolation.core.mq.MqInfo;
import com.netease.nim.camellia.mq.isolation.core.mq.TopicType;
import com.netease.nim.camellia.tools.executor.CamelliaThreadFactory;
import com.netease.nim.camellia.tools.utils.SysUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by caojiajun on 2024/2/7
 */
public abstract class AbstractConsumerManager {

    private static final Logger logger = LoggerFactory.getLogger(AbstractConsumerManager.class);
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(SysUtils.getCpuNum(),
            new CamelliaThreadFactory("camellia-mq-isolation-consumer-manager"));

    private final ConsumerManagerConfig config;
    private final MqIsolationController controller;
    private final String namespace;
    private final CamelliaMqIsolationConsumer consumer;
    private final Set<MqInfo> startedMqInfoSet = new HashSet<>();
    private final ReentrantLock lock = new ReentrantLock();
    private final AtomicBoolean initOk = new AtomicBoolean(false);

    public AbstractConsumerManager(ConsumerManagerConfig config) {
        this.config = config;
        this.namespace = config.getConsumerConfig().getNamespace();
        this.controller = config.getConsumerConfig().getController();
        this.consumer = new CamelliaMqIsolationConsumer(config.getConsumerConfig());
    }

    /**
     * 启动
     */
    public void start() {
        if (initOk.compareAndSet(false, true)) {
            boolean success = initConsumers();
            if (!success) {
                throw new IllegalArgumentException("mq isolation consumer start error");
            }
            scheduler.scheduleAtFixedRate(this::initConsumers,
                    config.getReloadConsumerIntervalSeconds(), config.getReloadConsumerIntervalSeconds(), TimeUnit.SECONDS);
            logger.info("mq isolation consumer manager start success");
        } else {
            throw new IllegalStateException("mq isolation consumer start twice!");
        }
    }

    /**
     * 由子类自行实现的start0方法，可能是kafka，也可能是rocketmq，或者其他
     * @param mqInfo mqInfo
     */
    protected abstract void start0(MqInfo mqInfo);

    /**
     * 在业务实现的start0方法中，在获取到消息后，需要调用本方法去执行实际的消费逻辑
     * @param mqInfo 来源mqInfo
     * @param data 数据
     * @return 消费结果
     */
    protected final CompletableFuture<Boolean> onMsg(MqInfo mqInfo, byte[] data) {
        return consumer.onMsg(mqInfo, data);
    }

    //启动消费线程
    private boolean initConsumers() {
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
                List<MqIsolationConfig.ManualConfig> manualConfigs = mqIsolationConfig.getManualConfigs();
                if (manualConfigs != null) {
                    for (MqIsolationConfig.ManualConfig manualConfig : manualConfigs) {
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
                        List<MqIsolationConfig.ManualConfig> manualConfigs = mqIsolationConfig.getManualConfigs();
                        if (manualConfigs != null) {
                            for (MqIsolationConfig.ManualConfig manualConfig : manualConfigs) {
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
                List<MqIsolationConfig.ManualConfig> manualConfigs = mqIsolationConfig.getManualConfigs();
                if (manualConfigs != null) {
                    for (MqIsolationConfig.ManualConfig manualConfig : manualConfigs) {
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
                        List<MqIsolationConfig.ManualConfig> manualConfigs = mqIsolationConfig.getManualConfigs();
                        if (manualConfigs != null) {
                            for (MqIsolationConfig.ManualConfig manualConfig : manualConfigs) {
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
                if (startedMqInfoSet.contains(mqInfo)) {
                    continue;
                }
                try {
                    logger.info("try start consumer, namespace = {}, mqInfo = {}", namespace, mqInfo);
                    start0(mqInfo);
                    logger.info("start consumer success, namespace = {}, mqInfo = {}", namespace, mqInfo);
                    startedMqInfoSet.add(mqInfo);
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
                for (MqInfo mqInfo : startedMqInfoSet) {
                    controller.heartbeat(namespace, mqInfo);
                }
            } catch (Exception e) {
                logger.error("heartbeat to controller error, namespace = {}", namespace, e);
            }
            lock.unlock();
        }
    }
}
