package com.netease.nim.camellia.console.service.vo;

import com.netease.nim.camellia.console.service.bo.TableRefBO;

import java.util.List;

/**
 * Create with IntelliJ IDEA
 *
 * @author ChenHongliang
 */
public class CamelliaTableRefPage {
    private List<TableRefBO> tableRefs;
    private Integer count;

    public List<TableRefBO> getTableRefs() {
        return tableRefs;
    }

    public void setTableRefs(List<TableRefBO> tableRefs) {
        this.tableRefs = tableRefs;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }
}
