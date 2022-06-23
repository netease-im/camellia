package com.netease.nim.camellia.dashboard.model;

import java.util.List;

/**
 * Create with IntelliJ IDEA
 *
 * @author ChenHongliang
 */
public class TableRefPage {
    private List<TableRefAddition> TableRefs;
    private Integer count;

    public List<TableRefAddition> getTableRefs() {
        return TableRefs;
    }

    public void setTableRefs(List<TableRefAddition> tableRefs) {
        TableRefs = tableRefs;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }
}
