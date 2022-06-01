package com.netease.nim.camellia.dashboard.model;

import java.util.List;

/**
 * Create with IntelliJ IDEA
 *
 * @author ChenHongliang
 * @date 2022/5/7 11:38
 */
public class TableWithTableRefs {
    private Table table;
    private List<TableRef> tableRefs;

    public Table getTable() {
        return table;
    }

    public void setTable(Table table) {
        this.table = table;
    }

    public List<TableRef> getTableRefs() {
        return tableRefs;
    }

    public void setTableRefs(List<TableRef> tableRefs) {
        this.tableRefs = tableRefs;
    }
}
