package com.netease.nim.camellia.hot.key.server.callback;

import com.netease.nim.camellia.hot.key.common.model.KeyAction;
import com.netease.nim.camellia.hot.key.common.model.Rule;

/**
 * Created by caojiajun on 2023/5/11
 */
public class HotKeyInfo {

    private final String namespace;
    private final String key;
    private final KeyAction action;
    private final Rule rule;
    private final long count;

    public HotKeyInfo(String namespace, String key, KeyAction action, Rule rule, long count) {
        this.namespace = namespace;
        this.key = key;
        this.action = action;
        this.rule = rule;
        this.count = count;
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
}
