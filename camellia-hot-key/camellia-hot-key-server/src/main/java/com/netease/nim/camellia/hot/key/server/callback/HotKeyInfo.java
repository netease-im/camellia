package com.netease.nim.camellia.hot.key.server.callback;

import com.netease.nim.camellia.hot.key.common.model.KeyAction;
import com.netease.nim.camellia.hot.key.common.model.Rule;

import java.util.List;

/**
 * Created by caojiajun on 2023/5/11
 */
public class HotKeyInfo {

    private final String namespace;
    private final String key;
    private final KeyAction action;
    private final Rule rule;
    private final long count;
    private final List<String> sources;

    public HotKeyInfo(String namespace, String key, KeyAction action, Rule rule, long count, List<String> sources) {
        this.namespace = namespace;
        this.key = key;
        this.action = action;
        this.rule = rule;
        this.count = count;
        this.sources = sources;
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

    public List<String> getSources() {
        return sources;
    }
}
