package com.netease.nim.camellia.hot.key.common.utils;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.netease.nim.camellia.hot.key.common.model.HotKeyConfig;
import com.netease.nim.camellia.hot.key.common.model.Rule;
import com.netease.nim.camellia.hot.key.common.model.RuleType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by caojiajun on 2023/5/11
 */
public class HotKeyConfigUtils {

    public static boolean isChange(HotKeyConfig oldConfig, HotKeyConfig newConfig) {
        if (oldConfig == null && newConfig != null) {
            return true;
        }
        if (oldConfig != null && newConfig == null) {
            return true;
        }
        return !JSONObject.toJSONString(oldConfig, SerializerFeature.SortField)
                .equals(JSONObject.toJSONString(newConfig, SerializerFeature.SortField));
    }

    public static boolean checkAndConvert(HotKeyConfig config) {
        if (config == null) {
            return false;
        }
        if (config.getNamespace() == null) {
            return false;
        }
        Set<String> nameSet = new HashSet<>();
        List<Rule> rules = config.getRules();
        if (rules != null) {
            for (Rule rule : rules) {
                if (!checkAndConvert(rule)) {
                    return false;
                }
                if (nameSet.contains(rule.getName())) {
                    return false;
                }
                nameSet.add(rule.getName());
            }
        }
        return true;
    }

    public static boolean checkAndConvert(Rule rule) {
        if (rule.getName() == null) return false;
        RuleType type = rule.getType();
        if (type == null) {
            return false;
        }
        if (rule.getName() == null) {
            return false;
        }
        if (type == RuleType.match_all) {
            rule.setKeyConfig(null);
        }
        if (type == RuleType.prefix_match || type == RuleType.exact_match || type == RuleType.contains) {
            if (rule.getKeyConfig() == null) {
                return false;
            }
        }
        Long checkMillis = rule.getCheckMillis();
        Long checkThreshold = rule.getCheckThreshold();
        if (checkMillis == null || checkThreshold == null) {
            return false;
        }
        if (checkMillis < 100) {
            return false;
        }
        long tmp = (checkMillis / 100L) * 100L;
        if (checkMillis - tmp > 50) {
            rule.setCheckMillis(tmp + 100);
        } else {
            rule.setCheckMillis(tmp);
        }
        return true;
    }
}
