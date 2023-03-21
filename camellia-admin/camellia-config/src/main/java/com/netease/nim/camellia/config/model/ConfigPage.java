package com.netease.nim.camellia.config.model;

import java.util.List;

/**
 * Created by caojiajun on 2023/3/21
 */
public class ConfigPage {

    private long count;
    private List<Config> list;

    public ConfigPage(long count, List<Config> list) {
        this.count = count;
        this.list = list;
    }

    public ConfigPage() {
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public List<Config> getList() {
        return list;
    }

    public void setList(List<Config> list) {
        this.list = list;
    }
}
