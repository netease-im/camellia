package com.netease.nim.camellia.mq.isolation.core.config;

import com.netease.nim.camellia.mq.isolation.core.mq.MqInfo;

import java.util.List;

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
    private static final double consumerSpendMsAvgThreshold = 300;

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
        if (config.getConsumerSpendMsAvgThreshold() == null) {
            config.setConsumerSpendMsAvgThreshold(consumerSpendMsAvgThreshold);
        }
        if (!checkPositiveNumber(config.getConsumerSpendMsAvgThreshold())) {
            throw new IllegalArgumentException("consumerSpendMsAvgThreshold is illegal");
        }

        if (!checkMqInfoList(config.getFast())) {
            throw new IllegalArgumentException("fast is illegal");
        }
        if (!checkMqInfoList(config.getFastError())) {
            throw new IllegalArgumentException("fastError is illegal");
        }
        if (!checkMqInfoList(config.getSlow())) {
            throw new IllegalArgumentException("slow is illegal");
        }
        if (!checkMqInfoList(config.getSlowError())) {
            throw new IllegalArgumentException("slowError is illegal");
        }
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
