package com.netease.nim.camellia.config.model;

import java.util.List;

/**
 * Created by caojiajun on 2023/3/21
 */
public class ConfigNamespacePage {
    private long count;
    private List<ConfigNamespace> list;

    public ConfigNamespacePage(long count, List<ConfigNamespace> list) {
        this.count = count;
        this.list = list;
    }

    public ConfigNamespacePage() {
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public List<ConfigNamespace> getList() {
        return list;
    }

    public void setList(List<ConfigNamespace> list) {
        this.list = list;
    }
}
