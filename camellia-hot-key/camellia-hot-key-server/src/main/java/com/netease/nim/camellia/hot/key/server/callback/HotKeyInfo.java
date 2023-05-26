package com.netease.nim.camellia.hot.key.server.callback;

import com.netease.nim.camellia.hot.key.common.model.KeyAction;
import com.netease.nim.camellia.hot.key.common.model.Rule;

import java.util.Set;

/**
 * Created by caojiajun on 2023/5/11
 */
public class HotKeyInfo {

    private final String namespace;
    private final String key;
    private final KeyAction action;
    private final Rule rule;
    private final long count;
    private final Set<String> sourceSet;

    public HotKeyInfo(String namespace, String key, KeyAction action, Rule rule, long count, Set<String> sourceSet) {
        this.namespace = namespace;
        this.key = key;
        this.action = action;
        this.rule = rule;
        this.count = count;
        this.sourceSet = sourceSet;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getKey() {
        return key;
    }

    public KeyAction getAction() {
        return action;
    }

    public Rule getRule() {
        return rule;
    }

    public long getCount() {
        return count;
    }

    public Set<String> getSourceSet() {
        return sourceSet;
    }
}
