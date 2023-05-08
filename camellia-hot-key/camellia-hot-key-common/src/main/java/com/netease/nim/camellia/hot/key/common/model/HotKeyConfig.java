package com.netease.nim.camellia.hot.key.common.model;

import java.util.List;

/**
 * Created by caojiajun on 2023/5/8
 */
public class HotKeyConfig {

    private String namespace;
    private NamespaceType type;
    private List<Rule> rules;

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public NamespaceType getType() {
        return type;
    }

    public void setType(NamespaceType type) {
        this.type = type;
    }

    public List<Rule> getRules() {
        return rules;
    }

    public void setRules(List<Rule> rules) {
        this.rules = rules;
    }
}
