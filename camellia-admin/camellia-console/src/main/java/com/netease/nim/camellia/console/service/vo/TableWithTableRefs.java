package com.netease.nim.camellia.console.service.vo;

import com.netease.nim.camellia.console.model.CamelliaDashboard;
import com.netease.nim.camellia.console.service.bo.TableBO;
import com.netease.nim.camellia.console.service.bo.TableRefBO;

import java.util.List;

/**
 * Create with IntelliJ IDEA
 *
 * @author ChenHongliang
 */
public class TableWithTableRefs {
    private CamelliaDashboard dashboard;
    private TableBO table;
    private List<TableRefBO> tableRefs;

    public TableBO getTable() {
        return table;
    }

    public void setTable(TableBO table) {
        this.table = table;
    }

    public List<TableRefBO> getTableRefs() {
        return tableRefs;
    }

    public void setTableRefs(List<TableRefBO> tableRefs) {
        this.tableRefs = tableRefs;
    }

    public CamelliaDashboard getDashboard() {
        return dashboard;
    }

    public void setDashboard(CamelliaDashboard dashboard) {
        this.dashboard = dashboard;
    }
}
