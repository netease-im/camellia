package com.netease.nim.camellia.hotkey.tests.support;

import com.netease.nim.camellia.hot.key.common.model.HotKeyConfig;
import com.netease.nim.camellia.hot.key.common.model.KeyAction;
import com.netease.nim.camellia.hot.key.common.model.KeyCounter;
import com.netease.nim.camellia.hot.key.common.model.Rule;
import com.netease.nim.camellia.hot.key.common.model.RuleType;

import java.util.Arrays;

public final class HotKeyTestFixtures {

    private HotKeyTestFixtures() {
    }

    public static Rule rule(String name, RuleType type, String keyConfig, long checkMillis, long checkThreshold, long expireMillis) {
        Rule rule = new Rule();
        rule.setName(name);
        rule.setType(type);
        rule.setKeyConfig(keyConfig);
        rule.setCheckMillis(checkMillis);
        rule.setCheckThreshold(checkThreshold);
        rule.setExpireMillis(expireMillis);
        return rule;
    }

    public static HotKeyConfig config(String namespace, Rule... rules) {
        HotKeyConfig config = new HotKeyConfig();
        config.setNamespace(namespace);
        config.setRules(Arrays.asList(rules));
        return config;
    }

    public static KeyCounter counter(String namespace, String key, KeyAction action, long count) {
        KeyCounter counter = new KeyCounter();
        counter.setNamespace(namespace);
        counter.setKey(key);
        counter.setAction(action);
        counter.setCount(count);
        return counter;
    }
}
