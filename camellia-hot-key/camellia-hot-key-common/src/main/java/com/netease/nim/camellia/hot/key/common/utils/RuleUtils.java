package com.netease.nim.camellia.hot.key.common.utils;

import com.netease.nim.camellia.hot.key.common.model.HotKeyConfig;
import com.netease.nim.camellia.hot.key.common.model.Rule;
import com.netease.nim.camellia.hot.key.common.model.RuleType;

import java.util.List;

/**
 * Created by caojiajun on 2023/5/9
 */
public class RuleUtils {

    public static Rule rulePass(HotKeyConfig hotKeyConfig, String key) {
        if (hotKeyConfig == null || key == null) return null;
        List<Rule> rules = hotKeyConfig.getRules();
        if (rules == null || rules.isEmpty()) {
            return null;
        }
        for (Rule rule : rules) {
            if (rulePass(rule, key)) {
                return rule;
            }
        }
        return null;
    }

    public static boolean rulePass(Rule rule, String key) {
        if (rule == null || key == null) return false;
        RuleType ruleType = rule.getType();
        if (ruleType == RuleType.exact_match) {
            return key.equals(rule.getKeyConfig());
        } else if (ruleType == RuleType.prefix_match) {
            return key.startsWith(rule.getKeyConfig());
        } else if (ruleType == RuleType.match_all) {
            return true;
        } else if (ruleType == RuleType.contains) {
            return key.contains(rule.getKeyConfig());
        }
        return false;
    }
}
