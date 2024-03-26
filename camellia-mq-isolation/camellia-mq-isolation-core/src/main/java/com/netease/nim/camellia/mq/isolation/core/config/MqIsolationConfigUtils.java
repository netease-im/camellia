package com.netease.nim.camellia.mq.isolation.core.config;

import com.netease.nim.camellia.mq.isolation.core.mq.MqInfo;
import com.netease.nim.camellia.mq.isolation.core.mq.TopicType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by caojiajun on 2024/2/20
 */
public class MqIsolationConfigUtils {

    private static final int senderStatsIntervalSeconds = 10;
    private static final int senderStatsExpireSeconds = 60;
    private static final int consumerStatsIntervalSeconds = 10;
    private static final int consumerStatsExpireSeconds = 60;
    private static final int senderHeavyTrafficThreshold1 = 5000;
    private static final int senderHeavyTrafficThreshold2 = 2000;
    private static final double senderHeavyTrafficPercent = 0.5;
    private static final double consumerFailRateThreshold = 0.5;

    public static void checkValid(MqIsolationConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("config is null");
        }
        String namespace = config.getNamespace();
        if (namespace == null) {
            throw new IllegalArgumentException("namespace is null");
        }

        //
        if (config.getSenderStatsIntervalSeconds() == null) {
            config.setSenderStatsIntervalSeconds(senderStatsIntervalSeconds);
        }
        if (!checkPositiveNumber(config.getSenderStatsIntervalSeconds())) {
            throw new IllegalArgumentException("senderStatsIntervalSeconds is illegal");
        }

        //
        if (config.getConsumerStatsIntervalSeconds() == null) {
            config.setConsumerStatsIntervalSeconds(consumerStatsIntervalSeconds);
        }
        if (!checkPositiveNumber(config.getConsumerStatsIntervalSeconds())) {
            throw new IllegalArgumentException("consumerStatsIntervalSeconds is illegal");
        }

        //
        if (config.getSenderStatsExpireSeconds() == null) {
            config.setSenderStatsExpireSeconds(senderStatsExpireSeconds);
        }
        if (!checkPositiveNumber(config.getSenderStatsExpireSeconds())) {
            throw new IllegalArgumentException("senderStatsExpireSeconds is illegal");
        }

        //
        if (config.getConsumerStatsExpireSeconds() == null) {
            config.setConsumerStatsExpireSeconds(consumerStatsExpireSeconds);
        }
        if (!checkPositiveNumber(config.getConsumerStatsExpireSeconds())) {
            throw new IllegalArgumentException("consumerStatsExpireSeconds is illegal");
        }

        //
        if (config.getSenderHeavyTrafficThreshold1() == null) {
            config.setSenderHeavyTrafficThreshold1(senderHeavyTrafficThreshold1);
        }
        if (!checkPositiveNumber(config.getSenderHeavyTrafficThreshold1())) {
            throw new IllegalArgumentException("senderHeavyTrafficThreshold1 is illegal");
        }

        //
        if (config.getSenderHeavyTrafficThreshold2() == null) {
            config.setSenderHeavyTrafficThreshold2(senderHeavyTrafficThreshold2);
        }
        if (!checkPositiveNumber(config.getSenderHeavyTrafficThreshold2())) {
            throw new IllegalArgumentException("senderHeavyTrafficThreshold2 is illegal");
        }

        //
        if (config.getSenderHeavyTrafficPercent() == null) {
            config.setSenderHeavyTrafficPercent(senderHeavyTrafficPercent);
        }
        if (!checkPositiveNumber(config.getSenderHeavyTrafficPercent())) {
            throw new IllegalArgumentException("senderHeavyTrafficPercent is illegal");
        }

        //
        if (config.getConsumerFailRateThreshold() == null) {
            config.setConsumerFailRateThreshold(consumerFailRateThreshold);
        }
        if (!checkPositiveNumber(config.getConsumerFailRateThreshold())) {
            throw new IllegalArgumentException("consumerFailRateThreshold is illegal");
        }

        //
        checkLevelInfoList(config.getLevelInfoList());
        checkLevelInfoList(config.getErrorLevelInfoList());

        if (!checkMqInfoList(config.getRetryLevel0())) {
            throw new IllegalArgumentException("retryLevel0 is illegal");
        }
        if (!checkMqInfoList(config.getRetryLevel1())) {
            throw new IllegalArgumentException("retryLevel1 is illegal");
        }
        if (!checkMqInfoList(config.getAutoIsolationLevel0())) {
            throw new IllegalArgumentException("autoIsolationLevel0 is illegal");
        }
        if (!checkMqInfoList(config.getAutoIsolationLevel1())) {
            throw new IllegalArgumentException("autoIsolationLevel1 is illegal");
        }

        List<ManualConfig> manualConfigs = config.getManualConfigs();
        if (manualConfigs != null) {
            for (ManualConfig manualConfig : manualConfigs) {
                if (manualConfig.getMatchType() == null) {
                    throw new IllegalArgumentException("ManualConfig.matchType is illegal");
                }
                if (manualConfig.getBizId() == null) {
                    throw new IllegalArgumentException("ManualConfig.bizId is illegal");
                }
                if (!checkMqInfo(manualConfig.getMqInfo())) {
                    throw new IllegalArgumentException("ManualConfig.MqInfo is illegal");
                }
            }
        }
        //check mq info
        topicTypeMap(config);
    }

    public static ConcurrentHashMap<MqInfo, TopicType> topicTypeMap(MqIsolationConfig config) {
        ConcurrentHashMap<MqInfo, TopicType> topicTypeMap = new ConcurrentHashMap<>();
        List<MqLevelInfo> levelInfoList = config.getLevelInfoList();
        for (MqLevelInfo mqLevelInfo : levelInfoList) {
            initMqInfo(topicTypeMap, mqLevelInfo.getMqInfoList(), TopicType.NORMAL);
        }
        List<MqLevelInfo> errorLevelInfoList = config.getErrorLevelInfoList();
        for (MqLevelInfo mqLevelInfo : errorLevelInfoList) {
            initMqInfo(topicTypeMap, mqLevelInfo.getMqInfoList(), TopicType.ERROR);
        }
        initMqInfo(topicTypeMap, config.getRetryLevel0(), TopicType.RETRY_LEVEL_0);
        initMqInfo(topicTypeMap, config.getRetryLevel1(), TopicType.RETRY_LEVEL_1);
        initMqInfo(topicTypeMap, config.getAutoIsolationLevel0(), TopicType.AUTO_ISOLATION_LEVEL_0);
        initMqInfo(topicTypeMap, config.getAutoIsolationLevel1(), TopicType.AUTO_ISOLATION_LEVEL_1);
        List<ManualConfig> manualConfigs = config.getManualConfigs();
        if (manualConfigs != null) {
            Set<MqInfo> mqInfoSet = new HashSet<>();
            for (ManualConfig manualConfig : manualConfigs) {
                MqInfo mqInfo = manualConfig.getMqInfo();
                mqInfoSet.add(mqInfo);
            }
            initMqInfo(topicTypeMap, mqInfoSet, TopicType.MANUAL_ISOLATION);
        }
        return topicTypeMap;
    }

    private static void checkLevelInfoList(List<MqLevelInfo> levelInfoList) {
        if (levelInfoList == null) {
            throw new IllegalArgumentException("levelInfoList is null");
        }
        if (levelInfoList.isEmpty()) {
            throw new IllegalArgumentException("levelInfoList is empty");
        }
        List<Long> list = new ArrayList<>();
        for (MqLevelInfo info : levelInfoList) {
            TimeRange timeRange = info.getTimeRange();
            long min = timeRange.getMin();
            long max = timeRange.getMax();
            list.add(min);
            list.add(max);
        }
        Collections.sort(list);
        if (list.get(0) > 0) {
            throw new IllegalArgumentException("time range not cover all");
        }
        if (list.get(list.size() - 1) != Long.MAX_VALUE) {
            throw new IllegalArgumentException("time range not cover all");
        }
        //0 1 2 3 4 5
        if (list.size() > 2) {
            for (int i=1; i<list.size() - 2; i+=2) {
                if (!Objects.equals(list.get(i), list.get(i + 1))) {
                    throw new IllegalArgumentException("time range not cover all");
                }
            }
        }
    }

    public static Set<MqInfo> getAllMqInfo(MqIsolationConfig config) {
        Set<MqInfo> set = new HashSet<>();
        List<MqLevelInfo> levelInfoList = config.getLevelInfoList();
        for (MqLevelInfo mqLevelInfo : levelInfoList) {
            set.addAll(mqLevelInfo.getMqInfoList());
        }
        List<MqLevelInfo> errorLevelInfoList = config.getErrorLevelInfoList();
        for (MqLevelInfo mqLevelInfo : errorLevelInfoList) {
            set.addAll(mqLevelInfo.getMqInfoList());
        }
        set.addAll(config.getRetryLevel0());
        set.addAll(config.getRetryLevel1());
        set.addAll(config.getAutoIsolationLevel0());
        set.addAll(config.getAutoIsolationLevel1());
        List<ManualConfig> manualConfigs = config.getManualConfigs();
        if (manualConfigs != null) {
            for (ManualConfig manualConfig : manualConfigs) {
                set.add(manualConfig.getMqInfo());
            }
        }
        return set;
    }

    public static Set<MqInfo> getMqInfoSetSpecifyTopicType(MqIsolationConfig config, TopicType type) {
        switch (type) {
            case NORMAL: {
                Set<MqInfo> set = new HashSet<>();
                List<MqLevelInfo> levelInfoList = config.getLevelInfoList();
                for (MqLevelInfo mqLevelInfo : levelInfoList) {
                    set.addAll(mqLevelInfo.getMqInfoList());
                }
                return set;
            }
            case ERROR: {
                Set<MqInfo> set = new HashSet<>();
                List<MqLevelInfo> levelInfoList = config.getErrorLevelInfoList();
                for (MqLevelInfo mqLevelInfo : levelInfoList) {
                    set.addAll(mqLevelInfo.getMqInfoList());
                }
                return set;
            }
            case RETRY_LEVEL_0:
                return new HashSet<>(config.getRetryLevel0());
            case RETRY_LEVEL_1:
                return new HashSet<>(config.getRetryLevel1());
            case AUTO_ISOLATION_LEVEL_0:
                return new HashSet<>(config.getAutoIsolationLevel0());
            case AUTO_ISOLATION_LEVEL_1:
                return new HashSet<>(config.getAutoIsolationLevel1());
            case MANUAL_ISOLATION: {
                List<ManualConfig> manualConfigs = config.getManualConfigs();
                Set<MqInfo> set = new HashSet<>();
                if (manualConfigs != null) {
                    for (ManualConfig manualConfig : manualConfigs) {
                        set.add(manualConfig.getMqInfo());
                    }
                }
                return set;
            }
            default:
                return new HashSet<>();
        }
    }

    public static Set<MqInfo> getMqInfoSetExcludeTopicType(MqIsolationConfig config, Collection<TopicType> excludeTopicTypeSet) {
        if (excludeTopicTypeSet == null || excludeTopicTypeSet.isEmpty()) {
            return getAllMqInfo(config);
        }
        Set<MqInfo> set = new HashSet<>();
        if (!excludeTopicTypeSet.contains(TopicType.NORMAL)) {
            List<MqLevelInfo> levelInfoList = config.getLevelInfoList();
            for (MqLevelInfo mqLevelInfo : levelInfoList) {
                set.addAll(mqLevelInfo.getMqInfoList());
            }
        }
        if (!excludeTopicTypeSet.contains(TopicType.ERROR)) {
            List<MqLevelInfo> levelInfoList = config.getLevelInfoList();
            for (MqLevelInfo mqLevelInfo : levelInfoList) {
                set.addAll(mqLevelInfo.getMqInfoList());
            }
        }
        if (!excludeTopicTypeSet.contains(TopicType.RETRY_LEVEL_0)) {
            set.addAll(config.getRetryLevel0());
        }
        if (!excludeTopicTypeSet.contains(TopicType.RETRY_LEVEL_1)) {
            set.addAll(config.getRetryLevel1());
        }
        if (!excludeTopicTypeSet.contains(TopicType.AUTO_ISOLATION_LEVEL_0)) {
            set.addAll(config.getAutoIsolationLevel0());
        }
        if (!excludeTopicTypeSet.contains(TopicType.AUTO_ISOLATION_LEVEL_1)) {
            set.addAll(config.getAutoIsolationLevel1());
        }
        if (!excludeTopicTypeSet.contains(TopicType.MANUAL_ISOLATION)) {
            List<ManualConfig> manualConfigs = config.getManualConfigs();
            if (manualConfigs != null) {
                for (ManualConfig manualConfig : manualConfigs) {
                    set.add(manualConfig.getMqInfo());
                }
            }
        }
        return set;
    }

    public static MqIsolationConfig toMqIsolationConfig(ReadableMqIsolationConfig config) {
        MqIsolationConfig mqIsolationConfig = new MqIsolationConfig();
        Map<String, String> mqAlias = config.getMqAlias();
        mqIsolationConfig.setNamespace(config.getNamespace());

        mqIsolationConfig.setConsumerFailRateThreshold(config.getConsumerFailRateThreshold());
        mqIsolationConfig.setConsumerStatsExpireSeconds(config.getConsumerStatsExpireSeconds());
        mqIsolationConfig.setConsumerStatsIntervalSeconds(config.getConsumerStatsIntervalSeconds());

        mqIsolationConfig.setSenderStatsIntervalSeconds(config.getSenderStatsIntervalSeconds());
        mqIsolationConfig.setSenderStatsExpireSeconds(config.getSenderStatsExpireSeconds());
        mqIsolationConfig.setSenderHeavyTrafficPercent(config.getSenderHeavyTrafficPercent());
        mqIsolationConfig.setSenderHeavyTrafficThreshold1(config.getSenderHeavyTrafficThreshold1());
        mqIsolationConfig.setSenderHeavyTrafficThreshold2(config.getSenderHeavyTrafficThreshold2());

        mqIsolationConfig.setLevelInfoList(config.getLevelInfoList());
        mqIsolationConfig.setErrorLevelInfoList(config.getErrorLevelInfoList());
        mqIsolationConfig.setRetryLevel0(config.getRetryLevel0());
        mqIsolationConfig.setRetryLevel1(config.getRetryLevel1());
        mqIsolationConfig.setAutoIsolationLevel0(config.getAutoIsolationLevel0());
        mqIsolationConfig.setAutoIsolationLevel1(config.getAutoIsolationLevel1());
        mqIsolationConfig.setManualConfigs(config.getManualConfigs());

        List<MqLevelInfo> levelInfoList = mqIsolationConfig.getLevelInfoList();
        for (MqLevelInfo levelInfo : levelInfoList) {
            replaceMqIfNeed(levelInfo.getMqInfoList(), mqAlias);
        }

        List<MqLevelInfo> errorLevelInfoList = mqIsolationConfig.getErrorLevelInfoList();
        for (MqLevelInfo levelInfo : errorLevelInfoList) {
            replaceMqIfNeed(levelInfo.getMqInfoList(), mqAlias);
        }

        replaceMqIfNeed(mqIsolationConfig.getRetryLevel0(), mqAlias);
        replaceMqIfNeed(mqIsolationConfig.getRetryLevel1(), mqAlias);
        replaceMqIfNeed(mqIsolationConfig.getAutoIsolationLevel0(), mqAlias);
        replaceMqIfNeed(mqIsolationConfig.getAutoIsolationLevel1(), mqAlias);

        List<ManualConfig> manualConfigs = config.getManualConfigs();
        if (manualConfigs != null && mqAlias != null) {
            for (ManualConfig manualConfig : manualConfigs) {
                MqInfo mqInfo = manualConfig.getMqInfo();
                String realMq = mqAlias.get(mqInfo.getMq());
                if (realMq != null) {
                    mqInfo.setMq(realMq);
                }
            }
        }
        return mqIsolationConfig;
    }

    private static void replaceMqIfNeed(List<MqInfo> mqInfos, Map<String, String> mqAlias) {
        if (mqInfos == null || mqInfos.isEmpty()) return;
        if (mqAlias == null || mqAlias.isEmpty()) return;
        for (MqInfo mqInfo : mqInfos) {
            String realMq = mqAlias.get(mqInfo.getMq());
            if (realMq != null) {
                mqInfo.setMq(realMq);
            }
        }
    }

    private static void initMqInfo(Map<MqInfo, TopicType> map, Collection<MqInfo> list, TopicType topicType) {
        for (MqInfo mqInfo : list) {
            if (map.containsKey(mqInfo)) {
                throw new IllegalArgumentException("duplicate mq info");
            }
            map.put(mqInfo, topicType);
        }
    }

    private static boolean checkPositiveNumber(Integer num) {
        return num != null && num > 0;
    }

    private static boolean checkPositiveNumber(Double num) {
        return num != null && num > 0;
    }

    private static boolean checkMqInfoList(List<MqInfo> list) {
        if (list == null) {
            return false;
        }
        if (list.isEmpty()) {
            return false;
        }
        for (MqInfo mqInfo : list) {
            if (!checkMqInfo(mqInfo)) {
                return false;
            }
        }
        return true;
    }

    private static boolean checkMqInfo(MqInfo mqInfo) {
        if (mqInfo == null) return false;
        if (mqInfo.getMq() == null) return false;
        if (mqInfo.getTopic() == null) return false;
        return true;
    }
}
