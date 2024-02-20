package com.netease.nim.camellia.mq.isolation.core.config;

import com.netease.nim.camellia.mq.isolation.core.mq.MqInfo;

import java.util.List;

/**
 * Created by caojiajun on 2024/2/20
 */
public class MqIsolationConfigUtils {

    public static boolean checkValid(MqIsolationConfig config) {
        String namespace = config.getNamespace();
        if (namespace == null) {
            return false;
        }
        if (!checkMqInfoList(config.getFast())) return false;
        if (!checkMqInfoList(config.getFastError())) return false;
        if (!checkMqInfoList(config.getSlow())) return false;
        if (!checkMqInfoList(config.getSlowError())) return false;
        if (!checkMqInfoList(config.getRetryLevel0())) return false;
        if (!checkMqInfoList(config.getRetryLevel1())) return false;
        if (!checkMqInfoList(config.getAutoIsolationLevel0())) return false;
        if (!checkMqInfoList(config.getAutoIsolationLevel0())) return false;
        List<ManualConfig> manualConfigs = config.getManualConfigs();
        if (manualConfigs != null) {
            for (ManualConfig manualConfig : manualConfigs) {
                if (manualConfig.getMatchType() == null) {
                    return false;
                }
                if (manualConfig.getBizId() == null) {
                    return false;
                }
                if (manualConfig.getMqInfo() == null) {
                    return false;
                }
                if (!checkMqInfo(manualConfig.getMqInfo())) {
                    return false;
                }
            }
        }
        return true;
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
