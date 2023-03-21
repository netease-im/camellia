package com.netease.nim.camellia.config.model;

import java.util.List;

/**
 * Created by caojiajun on 2023/3/21
 */
public class ConfigHistoryPage {
    private long count;
    private List<ConfigHistory> list;

    public ConfigHistoryPage(long count, List<ConfigHistory> list) {
        this.count = count;
        this.list = list;
    }

    public ConfigHistoryPage() {
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public List<ConfigHistory> getList() {
        return list;
    }

    public void setList(List<ConfigHistory> list) {
        this.list = list;
    }
}
