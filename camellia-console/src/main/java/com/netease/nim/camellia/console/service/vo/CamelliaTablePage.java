package com.netease.nim.camellia.console.service.vo;

import com.netease.nim.camellia.console.service.bo.TableBO;

import java.util.List;

/**
 * Create with IntelliJ IDEA
 *
 * @author ChenHongliang
 */
public class CamelliaTablePage {
    private List<TableBO> tables;
    private Integer count;

    public List<TableBO> getTables() {
        return tables;
    }

    public void setTables(List<TableBO> tables) {
        this.tables = tables;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }
}
