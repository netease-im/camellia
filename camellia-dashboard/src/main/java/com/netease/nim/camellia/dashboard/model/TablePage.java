package com.netease.nim.camellia.dashboard.model;

import java.util.List;

/**
 * Create with IntelliJ IDEA
 *
 * @author ChenHongliang
 * @date 2022/5/18 21:18
 */
public class TablePage {
    private List<Table> tables;
    private Integer count;

    public List<Table> getTables() {
        return tables;
    }

    public void setTables(List<Table> tables) {
        this.tables = tables;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }
}
